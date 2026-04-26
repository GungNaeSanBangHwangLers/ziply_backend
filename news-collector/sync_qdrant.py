"""
MySQL에 저장된 특정 구 뉴스를 Qdrant에 재동기화하는 스크립트.

사용법:
    python sync_qdrant.py --district 강북구
    python sync_qdrant.py --district 강북구 --dry-run
"""

import argparse
import logging
import sys

from sqlalchemy import select

from config import DATABASE_URL
from db_handler import SafetyNews, create_db_engine, get_session_factory
from qdrant_handler import QdrantNewsHandler

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
    handlers=[logging.StreamHandler(sys.stdout)],
)
logger = logging.getLogger(__name__)


def sync_district(district: str, dry_run: bool = False) -> None:
    engine = create_db_engine()
    SessionFactory = get_session_factory(engine)

    with SessionFactory() as session:
        rows = session.execute(
            select(SafetyNews).where(SafetyNews.region_name.like(f"%{district[:2]}%"))
        ).scalars().all()

    logger.info("MySQL에서 %s 관련 %d건 조회됨", district, len(rows))

    if not rows:
        logger.warning("조회된 데이터 없음. 구 이름 확인: %s", district)
        return

    if dry_run:
        logger.info("[DRY-RUN] Qdrant upsert 건너뜀")
        return

    qdrant = QdrantNewsHandler()

    news_items = []
    mysql_ids = []
    for row in rows:
        news_items.append({
            "title":          row.title,
            "content_url":    row.content_url or "",
            "published_at":   row.published_at,
            "category_level": row.category_level,
            "category_tag":   row.category_tag,
            "region_name":    row.region_name,
            "summary":        row.summary or "",
        })
        mysql_ids.append(row.id)

    saved = qdrant.bulk_upsert(news_items, mysql_ids)
    logger.info("Qdrant 동기화 완료: %d건 / 전체 %d건", saved, len(rows))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="MySQL → Qdrant 재동기화")
    parser.add_argument("--district", required=True, help="동기화할 구 이름 (예: 강북구)")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    sync_district(args.district, dry_run=args.dry_run)
