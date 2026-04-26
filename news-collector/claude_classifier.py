"""
Ziply 치안 뉴스 수집기 - OpenAI API 분류 모듈
뉴스 제목 + 설명을 기반으로 치안 위험도 레벨(1~3) + 중분류 태그를 분류한다.
"""

import json
import logging
import time
from typing import Literal

from openai import OpenAI, RateLimitError

# Rate limit 재시도 설정
_RATE_LIMIT_RETRIES = 4          # 최대 재시도 횟수
_RATE_LIMIT_BACKOFF = [60, 120, 240, 480]  # 대기 시간 (초)

from config import OPENAI_API_KEY, OPENAI_MODEL

logger = logging.getLogger(__name__)

ClassificationResult = Literal[1, 2, 3, "기타"]

_VALID_LEVELS = (1, 2, 3, "기타")

# 레벨별 허용 태그
_VALID_TAGS: dict[int, set[str]] = {
    1: {"기초질서 위반", "인근 소란", "유해 환경"},
    2: {"대인 불안", "재산 범죄", "질서 위반", "시설 위반"},
    3: {"침입 범죄", "대인 강력", "성범죄", "사회 재난"},
}

# 레벨에 맞지 않는 태그 → 가장 가까운 유효 태그로 교정
_TAG_RECOMMEND: dict[tuple[int, str], str] = {
    # Level 3 태그가 Level 2에 배정된 경우
    (2, "대인 강력"): "대인 불안",
    (2, "성범죄"):    "대인 불안",
    (2, "침입 범죄"): "재산 범죄",
    (2, "사회 재난"): "질서 위반",
    # Level 3 태그가 Level 1에 배정된 경우
    (1, "대인 강력"): "인근 소란",
    (1, "성범죄"):    "인근 소란",
    (1, "침입 범죄"): "유해 환경",
    (1, "사회 재난"): "유해 환경",
    # Level 2 태그가 Level 3에 배정된 경우
    (3, "재산 범죄"): "침입 범죄",
    (3, "대인 불안"): "대인 강력",
    (3, "질서 위반"): "사회 재난",
    (3, "시설 위반"): "침입 범죄",
    # Level 2 태그가 Level 1에 배정된 경우
    (1, "대인 불안"): "인근 소란",
    (1, "재산 범죄"): "유해 환경",
    (1, "질서 위반"): "기초질서 위반",
    (1, "시설 위반"): "유해 환경",
    # Level 1 태그가 Level 2에 배정된 경우
    (2, "기초질서 위반"): "질서 위반",
    (2, "인근 소란"):    "대인 불안",
    (2, "유해 환경"):    "질서 위반",
    # Level 1 태그가 Level 3에 배정된 경우
    (3, "기초질서 위반"): "사회 재난",
    (3, "인근 소란"):    "대인 강력",
    (3, "유해 환경"):    "사회 재난",
    # 완전히 잘못된 태그 (LLM 할루시네이션)
    (1, "부동산 범죄"): "유해 환경",
    (2, "부동산 범죄"): "재산 범죄",
    (3, "부동산 범죄"): "침입 범죄",
    # 오탈자 / 변형 태그
    (2, "대인지원"):   "대인 불안",
    (1, "대인지원"):   "인근 소란",
    (3, "대인지원"):   "대인 강력",
    (1, "사고"):       "기초질서 위반",
    (2, "사고"):       "질서 위반",
    (3, "사고"):       "사회 재난",
    (1, "명예훼손"):   "인근 소란",
    (2, "명예훼손"):   "질서 위반",
    (3, "명예훼손"):   "대인 강력",
    (1, "마약 관련"):  "유해 환경",
    (2, "마약 관련"):  "질서 위반",
    (3, "마약 관련"):  "사회 재난",
    (1, "조율 위반"):   "기초질서 위반",
    (2, "조율 위반"):   "질서 위반",
    (3, "조율 위반"):   "사회 재난",
    (1, "실시설 위반"): "유해 환경",
    (2, "실시설 위반"): "시설 위반",
    (3, "실시설 위반"): "침입 범죄",
    (1, "뇌물공여"):         "유해 환경",
    (2, "뇌물공여"):         "질서 위반",
    (3, "뇌물공여"):         "사회 재난",
    (1, "마약 의심 행위"):   "유해 환경",
    (2, "마약 의심 행위"):   "질서 위반",
    (3, "마약 의심 행위"):   "사회 재난",
    (1, "청소년 비행"):             "유해 환경",
    (2, "청소년 비행"):             "질서 위반",
    (3, "청소년 비행"):             "사회 재난",
    (1, "청소년 비행(흡연/모임)"): "유해 환경",
    (2, "청소년 비행(흡연/모임)"): "질서 위반",
    (3, "청소년 비행(흡연/모임)"): "사회 재난",
    (1, "강도"):                      "유해 환경",
    (2, "강도"):                      "재산 범죄",
    (3, "강도"):                      "침입 범죄",
    (2, "음주운전 사고 빈발 구역"):   "질서 위반",
    (1, "음주운전 사고 빈발 구역"):   "기초질서 위반",
    (3, "음주운전 사고 빈발 구역"):   "사회 재난",
    (1, "층간소음 분쟁"):             "인근 소란",
    (2, "층간소음 분쟁"):             "대인 불안",
    (3, "층간소음 분쟁"):             "대인 강력",
    (1, "아동 대상 범죄"):            "유해 환경",
    (2, "아동 대상 범죄"):            "대인 불안",
    (3, "아동 대상 범죄"):            "성범죄",
    # 레벨 대분류명을 태그로 잘못 사용한 경우
    (1, "생활 불편"):              "기초질서 위반",
    (1, "생활 불편 / 무질서"):     "기초질서 위반",
    (2, "안전 불안"):              "대인 불안",
    (2, "안전 불안 / 재산 위협"):  "대인 불안",
    (3, "신변 위협"):              "대인 강력",
    (3, "신변 위협 / 강력 범죄"):  "대인 강력",
}

# ── 분류 기준 (프롬프트 공통 블록) ─────────────────────────

_CRITERIA = """\
## 분류 기준

### Level 1 — 생활 불편 / 무질서
- **기초질서 위반**: 노상방뇨, 오물 투기, 쓰레기 무단 투기, 무단 횡단
- **인근 소란**: 고성방가, 공공장소 구걸, 노상 잠자기, 층간소음 분쟁
- **유해 환경**: 불법 전단지 배포, 청소년 비행(흡연/모임), 금연 구역 내 흡연

### Level 2 — 안전 불안 / 재산 위협
- **대인 불안**: 취객 난동, 위협적인 접근, 스토킹 전조 행위, 행패 소란
- **재산 범죄**: 자전거/오토바이 절도, 택배 도난, 차량 털이, 기물 파손(그래피티 등)
- **질서 위반**: 불법 도박(도박장 운영), 음주운전 사고 빈발 구역, 마약 의심 행위
- **시설 위반**: 무단 주거지 출입(공용부), 방범창 파손

### Level 3 — 신변 위협 / 강력 범죄
- **침입 범죄**: 주거 침입, 강도, 빈집털이(침입형 절도)
- **대인 강력**: 폭행 및 상해, 살인, 납치, 감금
- **성범죄**: 성폭행, 강제추행, 디지털 성범죄(몰카 등), 아동 대상 범죄
- **사회 재난**: 방화(불지르기), 흉기 난동, 보복 범죄

### 기타
부동산 광고, 맛집 홍보, 행정 공지, 교통 정보, 스포츠 기사 등
치안·범죄·사고와 무관한 콘텐츠"""

# ── 단건 시스템 프롬프트 ────────────────────────────────────

SYSTEM_PROMPT = f"""\
당신은 서울시 치안 뉴스를 분석하는 전문 분류 AI입니다.
뉴스 제목과 설명을 읽고 아래 기준에 따라 정확하게 분류하세요.

{_CRITERIA}

## 응답 규칙
- 반드시 JSON 형식으로만 응답하세요.
- level: 1, 2, 3 (정수) 또는 "기타" (문자열)
- category_tag: 해당 레벨의 중분류 이름 (기타는 null)
- summary: 기사 핵심 내용을 한국어 1~2 문장으로 요약 (기타는 null)
- 판단 근거가 불충분하면 가장 낮은 레벨로 분류하세요.

응답 예시:
{{"level": 2, "category_tag": "재산 범죄", "summary": "서울 강남구에서 택배 도난 사건이 잇따라 발생했다."}}
{{"level": "기타", "category_tag": null, "summary": null}}
"""

# ── 배치 시스템 프롬프트 ────────────────────────────────────

BATCH_SYSTEM_PROMPT = f"""\
당신은 서울시 치안 뉴스를 분석하는 전문 분류 AI입니다.
번호가 매겨진 뉴스 목록을 읽고, 아래 기준에 따라 각각을 정확하게 분류하세요.

{_CRITERIA}

## 응답 규칙
- 반드시 JSON 형식으로만 응답하세요.
- {{"results": [{{"id": <번호>, "level": <레벨>, "category_tag": <중분류>, "summary": <요약>}}, ...]}} 형식으로 응답하세요.
- level: 1, 2, 3 (정수) 또는 "기타" (문자열)
- category_tag: 해당 레벨의 중분류 이름 (기타는 null)
  - Level 1 허용값: "기초질서 위반", "인근 소란", "유해 환경"
  - Level 2 허용값: "대인 불안", "재산 범죄", "질서 위반", "시설 위반"
  - Level 3 허용값: "침입 범죄", "대인 강력", "성범죄", "사회 재난"
- summary: 기사 핵심 내용을 한국어 1~2 문장으로 요약 (기타는 null)
- 판단 근거가 불충분하면 가장 낮은 레벨로 분류하세요.
- 입력된 모든 기사 번호에 대한 결과를 빠짐없이 포함하세요.
"""


def _validate_tag(level: int, tag: str | None) -> str | None:
    """레벨에 맞지 않는 태그는 가장 가까운 유효 태그로 교정한다."""
    if tag is None:
        return None
    if tag in _VALID_TAGS.get(level, set()):
        return tag
    recommended = _TAG_RECOMMEND.get((level, tag))
    if recommended:
        logger.info("레벨 %d 태그 교정: '%s' → '%s'", level, tag, recommended)
        return recommended
    logger.warning("레벨 %d에 맞지 않는 태그 '%s', null 처리", level, tag)
    return None


class ClaudeClassifier:
    """OpenAI API를 사용해 뉴스를 치안 레벨 + 중분류로 분류한다."""

    def __init__(self) -> None:
        if not OPENAI_API_KEY:
            raise EnvironmentError("OPENAI_API_KEY 환경변수가 설정되지 않았습니다.")
        self._client = OpenAI(api_key=OPENAI_API_KEY)

    def classify(self, title: str, description: str, region: str) -> dict:
        """
        단건 뉴스를 분류한다.

        Returns:
            {"level": int|"기타", "category_tag": str|None, "summary": str|None}
        """
        user_message = (
            f"지역: {region}\n"
            f"제목: {title}\n"
            f"내용: {description or '(내용 없음)'}"
        )

        try:
            response = self._client.chat.completions.create(
                model=OPENAI_MODEL,
                max_tokens=300,
                response_format={"type": "json_object"},
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": user_message},
                ],
            )
            raw = response.choices[0].message.content.strip()
            result = json.loads(raw)

            level = result.get("level")
            tag   = result.get("category_tag")
            summary = result.get("summary")

            # 문자열 숫자 → int 정규화
            if isinstance(level, str) and level.isdigit():
                level = int(level)

            if level not in _VALID_LEVELS:
                logger.warning("알 수 없는 레벨값 '%s', 기타로 처리", level)
                return {"level": "기타", "category_tag": None, "summary": None}

            if level != "기타":
                tag = _validate_tag(level, tag)

            return {"level": level, "category_tag": tag, "summary": summary}

        except json.JSONDecodeError:
            logger.warning("JSON 파싱 실패, 기타로 처리. 원본: %s", raw[:100])
            return {"level": "기타", "category_tag": None, "summary": None}
        except Exception as e:
            logger.error("분류 오류: %s", e)
            return {"level": "기타", "category_tag": None, "summary": None}

    def classify_batch(self, articles: list[dict]) -> list[dict]:
        """
        여러 기사를 한 번의 API 호출로 분류한다 (진짜 배치 처리).
        실패 시 개별 분류로 폴백.

        Returns:
            category_level, category_tag, summary 필드가 추가된 article dict 목록 (기타 제외)
        """
        if not articles:
            return []

        parts = []
        for i, article in enumerate(articles, 1):
            parts.append(
                f"[{i}]\n"
                f"지역: {article['region_name']}\n"
                f"제목: {article['title']}\n"
                f"내용: {article.get('description', '(내용 없음)')}"
            )
        user_message = "\n\n".join(parts)

        raw = ""
        for attempt in range(_RATE_LIMIT_RETRIES + 1):
            try:
                response = self._client.chat.completions.create(
                    model=OPENAI_MODEL,
                    max_tokens=min(200 * len(articles), 4096),
                    response_format={"type": "json_object"},
                    messages=[
                        {"role": "system", "content": BATCH_SYSTEM_PROMPT},
                        {"role": "user", "content": user_message},
                    ],
                )
                raw = response.choices[0].message.content.strip()
                data = json.loads(raw)

                # results 키가 없거나 최상위 배열로 반환되는 경우 대응
                results_list = data.get("results") or data.get("items") or (data if isinstance(data, list) else [])
                if not results_list:
                    logger.warning("배치 응답에 results 없음, 원본: %s", raw[:200])
                    return self._classify_individually(articles)

                # id가 int 또는 str로 올 수 있으므로 int로 정규화
                result_map = {int(r["id"]): r for r in results_list if "id" in r}

                classified = []
                for i, article in enumerate(articles, 1):
                    r = result_map.get(i, {})
                    level   = r.get("level")
                    tag     = r.get("category_tag")
                    summary = r.get("summary")

                    # 문자열 숫자 → int 정규화 ('2' → 2)
                    if isinstance(level, str) and level.isdigit():
                        level = int(level)

                    if level not in _VALID_LEVELS:
                        logger.warning("배치 [%d] 알 수 없는 레벨값 '%s', 기타로 처리", i, level)
                        continue

                    if level == "기타":
                        logger.debug("기타 분류, 저장 제외: %s", article["title"][:50])
                        continue

                    article["category_level"] = level
                    article["category_tag"]   = _validate_tag(level, tag)
                    article["summary"]        = summary
                    classified.append(article)

                return classified

            except RateLimitError as e:
                if attempt < _RATE_LIMIT_RETRIES:
                    wait = _RATE_LIMIT_BACKOFF[attempt]
                    logger.warning(
                        "Rate limit 초과 (시도 %d/%d), %d초 대기 후 재시도...",
                        attempt + 1, _RATE_LIMIT_RETRIES, wait,
                    )
                    time.sleep(wait)
                else:
                    logger.error("Rate limit 재시도 횟수 초과, 배치 건너뜀: %s", e)
                    return []

            except json.JSONDecodeError:
                logger.warning("배치 JSON 파싱 실패, 개별 분류로 폴백. 원본: %s", raw[:200])
                return self._classify_individually(articles)
            except Exception as e:
                logger.error("배치 분류 오류, 개별 분류로 폴백: %s", e)
                return self._classify_individually(articles)

        return []

    def _classify_individually(self, articles: list[dict]) -> list[dict]:
        """배치 실패 시 개별 분류로 폴백."""
        classified = []
        for article in articles:
            result = self.classify(
                title=article["title"],
                description=article.get("description", ""),
                region=article["region_name"],
            )
            if result["level"] == "기타":
                continue
            article["category_level"] = result["level"]
            article["category_tag"]   = result["category_tag"]
            article["summary"]        = result["summary"]
            classified.append(article)
        return classified
