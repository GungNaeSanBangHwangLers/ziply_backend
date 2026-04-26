"""
Ziply 치안 뉴스 수집기 - 메인 파이프라인 실행 스크립트
서울시 25개 구 단위, 1년치 뉴스 수집

실행 예시:
    python main.py                          # 전체 25개 구 수집
    python main.py --district 동작구        # 특정 구만 수집
    python main.py --district 동작구 --dry-run  # DB 저장 없이 결과 확인
"""

import argparse
import logging
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime

from claude_classifier import ClaudeClassifier
from config import SEOUL_DISTRICTS
from db_handler import (
    create_db_engine,
    deduplicate_by_title,
    ensure_table_exists,
    get_session_factory,
    upsert_news,
)
from naver_collector import NaverNewsClient
from qdrant_handler import QdrantNewsHandler

# ── 로깅 설정 ─────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler("collector.log", encoding="utf-8"),
    ],
)
logger = logging.getLogger(__name__)

# 한 번의 OpenAI 호출에 넣을 기사 수 (프롬프트 토큰 한도 고려)
BATCH_SIZE = 20
# 동시에 실행할 OpenAI 배치 호출 수 (rate limit 여유분 확보)
MAX_CONCURRENT_BATCHES = 5


def _process_district(
    district: str,
    articles: list[dict],
    classifier: ClaudeClassifier,
    SessionFactory,
    qdrant: QdrantNewsHandler | None,
    dry_run: bool,
) -> tuple[int, int]:
    """
    수집된 기사 목록을 분류하고 MySQL + Qdrant에 저장한다.
    Returns: (classified_count, saved_count)
    """
    if not articles:
        return 0, 0

    articles = deduplicate_by_title(articles)

    batches = [articles[i : i + BATCH_SIZE] for i in range(0, len(articles), BATCH_SIZE)]
    logger.info("%s 분류 시작: %d건 → %d배치 (동시 %d)", district, len(articles), len(batches), MAX_CONCURRENT_BATCHES)

    classified: list[dict] = []
    with ThreadPoolExecutor(max_workers=MAX_CONCURRENT_BATCHES) as pool:
        futures = {pool.submit(classifier.classify_batch, batch): idx for idx, batch in enumerate(batches)}
        for future in as_completed(futures):
            classified.extend(future.result())

    excluded = len(articles) - len(classified)
    logger.info(
        "%s 분류 완료: %d건 (치안) / %d건 제외(기타)",
        district, len(classified), excluded,
    )

    if dry_run:
        for art in classified:
            logger.info(
                "[DRY-RUN] Lv%d | %s | %s",
                art["category_level"], art["region_name"], art["title"][:60],
            )
        return len(classified), 0

    with SessionFactory() as session:
        saved_records = _bulk_upsert_with_ids(session, classified)

    saved_count = len(saved_records)
    logger.info("%s MySQL 저장 완료: %d건 (신규)", district, saved_count)

    if qdrant and saved_records:
        items = [r["item"] for r in saved_records]
        ids   = [r["id"]   for r in saved_records]
        vec_saved = qdrant.bulk_upsert(items, ids)
        logger.info("%s Qdrant 저장 완료: %d건", district, vec_saved)

    return len(classified), saved_count


def _bulk_upsert_with_ids(session, news_list: list[dict]) -> list[dict]:
    results = []
    for item in news_list:
        record = upsert_news(session, item)
        if record is not None:
            results.append({"id": record.id, "item": item})
    try:
        session.commit()
    except Exception:
        session.rollback()
        raise
    return results


def run_pipeline(
    districts: list[str] | None = None,
    from_district: str | None = None,
    dry_run: bool = False,
) -> None:
    """
    서울시 구 단위 1년치 뉴스 수집 파이프라인.

    Args:
        districts: 수집할 구 목록 (None이면 25개 전체)
        from_district: 이 구부터 이어서 수집 (앞 구는 건너뜀)
        dry_run: True면 DB 저장 생략
    """
    start_time = datetime.now()
    target = districts or SEOUL_DISTRICTS

    if from_district:
        if from_district not in target:
            logger.error("알 수 없는 구 이름: %s", from_district)
            sys.exit(1)
        skip_idx = target.index(from_district)
        skipped = target[:skip_idx]
        target = target[skip_idx:]
        if skipped:
            logger.info("건너뜀 (이미 수집됨): %s", ", ".join(skipped))
    logger.info("=== Ziply 치안 뉴스 수집 시작 (%s) ===", start_time.strftime("%Y-%m-%d %H:%M"))
    logger.info("대상 구: %d개 | dry-run: %s", len(target), dry_run)

    collector = NaverNewsClient()
    classifier = ClaudeClassifier()

    if not dry_run:
        engine = create_db_engine()
        ensure_table_exists(engine)
        SessionFactory = get_session_factory(engine)
        try:
            qdrant = QdrantNewsHandler()
            logger.info("Qdrant 연결 성공")
        except Exception as e:
            logger.warning("Qdrant 연결 실패, 벡터 저장 건너뜀: %s", e)
            qdrant = None
    else:
        SessionFactory = None
        qdrant = None
        logger.info("[DRY-RUN] DB/Qdrant 저장을 건너뜁니다.")

    total_collected = total_classified = total_saved = 0

    for district in target:
        logger.info("────── [구] %s ──────", district)
        articles = list(collector.collect_district(district))
        total_collected += len(articles)
        logger.info("%s 수집: %d건", district, len(articles))

        c, s = _process_district(district, articles, classifier, SessionFactory, qdrant, dry_run)
        total_classified += c
        total_saved += s

    elapsed = (datetime.now() - start_time).seconds
    logger.info("══════════════════════════════════════")
    logger.info("수집 완료 요약")
    logger.info("  총 수집:  %d건", total_collected)
    logger.info("  총 분류:  %d건 (치안 관련)", total_classified)
    if not dry_run:
        logger.info("  총 저장:  %d건 (신규)", total_saved)
    logger.info("  소요시간: %d초", elapsed)
    logger.info("══════════════════════════════════════")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Ziply 서울 치안 뉴스 수집 파이프라인 (구 단위, 1년치)",
        formatter_class=argparse.RawTextHelpFormatter,
    )
    parser.add_argument(
        "--district",
        type=str,
        default=None,
        help="특정 구만 수집 (예: 동작구). 생략 시 25개 전체 수집.",
    )
    parser.add_argument(
        "--from-district",
        type=str,
        default=None,
        dest="from_district",
        help="이 구부터 이어서 수집 (예: 마포구). 앞 구는 건너뜀.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        default=False,
        help="DB에 저장하지 않고 분류 결과만 로그로 출력.",
    )
    args = parser.parse_args()

    districts = None
    if args.district:
        if args.district not in SEOUL_DISTRICTS:
            logger.error("알 수 없는 구 이름: %s", args.district)
            logger.info("유효한 구 목록: %s", ", ".join(SEOUL_DISTRICTS))
            sys.exit(1)
        districts = [args.district]

    run_pipeline(districts=districts, from_district=args.from_district, dry_run=args.dry_run)


if __name__ == "__main__":
    main()
