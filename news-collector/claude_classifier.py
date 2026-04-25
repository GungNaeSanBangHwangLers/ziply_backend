"""
Ziply 치안 뉴스 수집기 - OpenAI API 분류 모듈
뉴스 제목 + 설명을 기반으로 치안 위험도 레벨(1~3) 또는 '기타'를 분류한다.
"""

import json
import logging
from typing import Literal

from openai import OpenAI

from config import OPENAI_API_KEY, OPENAI_MODEL

logger = logging.getLogger(__name__)

ClassificationResult = Literal[1, 2, 3, "기타"]


# ── 시스템 프롬프트 ────────────────────────────────────────

SYSTEM_PROMPT = """\
당신은 서울시 치안 뉴스를 분석하는 전문 분류 AI입니다.
뉴스 제목과 설명을 읽고 아래 기준에 따라 정확하게 분류하세요.

## 분류 기준

**Level 1 (생활 불편)**
쓰레기 무단투기, 노상방뇨, 고성방가, 층간소음, 청소년 비행 등
실생활 불편을 유발하지만 직접적 신체 위협은 없는 사건

**Level 2 (안전 불안)**
취객 난동, 스토킹 전조, 자전거/택배 절도, 기물 파손, 음주운전 등
재산 피해 또는 잠재적 위협이 있는 사건

**Level 3 (신변 위협)**
주거 침입, 강도, 살인, 성범죄(몰카 포함), 방화, 흉기 난동 등
생명·신체에 직접 위협을 가하는 중대 범죄

**기타**
부동산 광고, 맛집 홍보, 행정 공지, 교통 정보, 스포츠 기사 등
치안/범죄/사고와 무관한 콘텐츠

## 응답 규칙
- 반드시 JSON 형식으로만 응답하세요.
- level 필드: 1, 2, 3 (정수) 또는 "기타" (문자열)
- summary 필드: 기사 핵심 내용을 한국어 1~2 문장으로 요약 (기타는 null)
- 판단 근거가 불충분하면 가장 낮은 레벨로 분류하세요.

응답 예시:
{"level": 2, "summary": "서울 마포구에서 취객이 편의점 직원에게 행패를 부려 경찰에 인계됐다."}
{"level": "기타", "summary": null}
"""


class ClaudeClassifier:
    """OpenAI API를 사용해 뉴스를 치안 레벨로 분류한다."""

    def __init__(self) -> None:
        if not OPENAI_API_KEY:
            raise EnvironmentError(
                "OPENAI_API_KEY 환경변수가 설정되지 않았습니다."
            )
        self._client = OpenAI(api_key=OPENAI_API_KEY)

    def classify(self, title: str, description: str, region: str) -> dict:
        """
        단건 뉴스를 분류한다.

        Args:
            title: 뉴스 제목
            description: 뉴스 설명 (네이버 API snippet)
            region: 검색에 사용된 지역구 이름

        Returns:
            {
                "level": int | "기타",
                "summary": str | None,
            }
        """
        user_message = (
            f"지역: {region}\n"
            f"제목: {title}\n"
            f"내용: {description or '(내용 없음)'}"
        )

        try:
            response = self._client.chat.completions.create(
                model=OPENAI_MODEL,
                max_tokens=256,
                response_format={"type": "json_object"},
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": user_message},
                ],
            )
            raw = response.choices[0].message.content.strip()

            result = json.loads(raw)
            level = result.get("level")
            summary = result.get("summary")

            if level not in (1, 2, 3, "기타"):
                logger.warning("알 수 없는 레벨값 '%s', 기타로 처리", level)
                level = "기타"
                summary = None

            return {"level": level, "summary": summary}

        except json.JSONDecodeError:
            logger.warning("JSON 파싱 실패, 기타로 처리. 원본: %s", raw[:100])
            return {"level": "기타", "summary": None}
        except Exception as e:
            logger.error("분류 오류: %s", e)
            return {"level": "기타", "summary": None}

    def classify_batch(self, articles: list[dict]) -> list[dict]:
        """
        여러 기사를 순차적으로 분류한다.
        각 기사 dict에 'level', 'summary' 필드를 추가하여 반환.

        Args:
            articles: naver_collector에서 반환된 raw article dict 목록

        Returns:
            분류 결과가 추가된 article dict 목록 (기타 제외)
        """
        classified = []
        for article in articles:
            result = self.classify(
                title=article["title"],
                description=article.get("description", ""),
                region=article["region_name"],
            )
            if result["level"] == "기타":
                logger.debug("기타 분류, 저장 제외: %s", article["title"][:50])
                continue

            article["category_level"] = result["level"]
            article["summary"] = result["summary"]
            classified.append(article)

        return classified
