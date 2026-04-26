"""
category_tag가 null인 기존 레코드를 재분류해서 MySQL + Qdrant 업데이트.

사용법:
    python fix_null_tags.py             # 전체 null 레코드 처리
    python fix_null_tags.py --dry-run   # 업데이트 없이 건수만 확인
"""

import argparse
import logging
import sys

from sqlalchemy import select, update

from claude_classifier import ClaudeClassifier
from db_handler import SafetyNews, create_db_engine, get_session_factory
from qdrant_handler import QdrantNewsHandler

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
    handlers=[logging.StreamHandler(sys.stdout)],
)
logger = logging.getLogger(__name__)

BATCH_SIZE = 20


def fix_null_tags(dry_run: bool = False) -> None:
    engine = create_db_engine()
    SessionFactory = get_session_factory(engine)

    with SessionFactory() as session:
        rows = session.execute(
            select(SafetyNews).where(
                SafetyNews.category_tag.is_(None),
                SafetyNews.category_level.in_([1, 2, 3]),
            )
        ).scalars().all()

    total = len(rows)
    logger.info("category_tag null 레코드: %d건", total)

    if total == 0:
        logger.info("수정할 레코드 없음.")
        return

    if dry_run:
        logger.info("[DRY-RUN] 업데이트 건너뜀.")
        return

    classifier = ClaudeClassifier()

    try:
        qdrant = QdrantNewsHandler()
        qdrant_enabled = True
    except Exception as e:
        logger.warning("Qdrant 연결 실패, Qdrant 업데이트 건너뜀: %s", e)
        qdrant_enabled = False

    updated = 0
    batches = [rows[i: i + BATCH_SIZE] for i in range(0, len(rows), BATCH_SIZE)]

    for batch_idx, batch in enumerate(batches, 1):
        articles = [
            {
                "title":       row.title,
                "description": row.summary or "",
                "region_name": row.region_name or "",
            }
            for row in batch
        ]

        results = classifier.classify_batch(articles)
        # classify_batch는 기타를 제외하고 반환하므로 id 매핑이 필요
        # 여기서는 단건 분류로 확실하게 처리
        for row in batch:
            result = classifier.classify(
                title=row.title,
                description=row.summary or "",
                region=row.region_name or "",
            )
            tag = result.get("category_tag")
            if not tag:
                logger.debug("재분류 후에도 tag 없음: id=%d", row.id)
                continue

            with SessionFactory() as session:
                session.execute(
                    update(SafetyNews)
                    .where(SafetyNews.id == row.id)
                    .values(category_tag=tag)
                )
                session.commit()

            if qdrant_enabled:
                qdrant.upsert(
                    news_item={
                        "title":          row.title,
                        "content_url":    row.content_url or "",
                        "published_at":   row.published_at,
                        "category_level": row.category_level,
                        "category_tag":   tag,
                        "region_name":    row.region_name or "",
                        "summary":        row.summary or "",
                    },
                    mysql_id=row.id,
                )

            updated += 1

        logger.info("배치 %d/%d 완료 (누적 업데이트: %d건)", batch_idx, len(batches), updated)

    logger.info("완료: %d건 업데이트 / 전체 null %d건", updated, total)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="category_tag null 레코드 재분류 업데이트")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    fix_null_tags(dry_run=args.dry_run)
