"""
Ziply 치안 뉴스 수집기 - DB 연동 (SQLAlchemy)
safety_news 테이블 모델 정의 및 CRUD 처리

중복 방지 전략 (3단계):
  L1 - content_url UNIQUE (URL 레벨)
  L2 - title_hash  UNIQUE (정규화 제목 해시 레벨)
  L3 - Jaccard 유사도 배치 내 필터 (main.py에서 수행)
"""

import hashlib
import logging
import re
from datetime import datetime
from typing import Optional

from sqlalchemy import (
    BigInteger, Column, DateTime, Index, SmallInteger,
    String, Text, UniqueConstraint, create_engine,
)
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from config import DATABASE_URL

logger = logging.getLogger(__name__)


# ── 제목 정규화 ───────────────────────────────────────────
_NON_WORD = re.compile(r"[^\w가-힣]")   # 특수문자·공백·영문 제거

def _normalize_title(title: str) -> str:
    """제목에서 불필요한 문자를 제거하고 소문자 변환한다."""
    return _NON_WORD.sub("", title).lower().strip()

def make_title_hash(title: str) -> str:
    """정규화된 제목의 SHA-256 앞 32자를 반환한다."""
    normalized = _normalize_title(title)
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()[:32]


# ── ORM 모델 ──────────────────────────────────────────────

class Base(DeclarativeBase):
    pass


class SafetyNews(Base):
    """
    Java 서버와 공유하는 safety_news 테이블.
    중복 방지 키:
      - content_url UNIQUE (URL 레벨)
      - title_hash  UNIQUE (정규화 제목 해시 레벨)
    """
    __tablename__ = "safety_news"

    id            = Column(BigInteger, primary_key=True, autoincrement=True)
    title         = Column(String(255), nullable=False)
    content_url   = Column(String(500), unique=True, nullable=True)
    title_hash    = Column(String(32), unique=True, nullable=True)  # L2 중복 방지
    published_at  = Column(DateTime, nullable=False)
    category_level = Column(SmallInteger, nullable=False)    # 1, 2, 3
    region_name   = Column(String(50), nullable=True)        # '동작구', '상도동' 등
    summary       = Column(Text, nullable=True)              # LLM 요약
    created_at    = Column(DateTime, default=datetime.utcnow)

    __table_args__ = (
        Index("idx_region_date", "region_name", "published_at"),
    )

    def __repr__(self) -> str:
        return (
            f"<SafetyNews id={self.id} level={self.category_level} "
            f"region={self.region_name} title={self.title[:30]}>"
        )


# ── 엔진 / 세션 팩토리 ────────────────────────────────────

def create_db_engine():
    engine = create_engine(
        DATABASE_URL,
        pool_pre_ping=True,
        pool_recycle=3600,
        echo=False,
    )
    return engine


def ensure_table_exists(engine) -> None:
    """safety_news 테이블이 없으면 생성."""
    Base.metadata.create_all(engine)
    logger.info("safety_news 테이블 준비 완료")


# ── L3: Jaccard 유사도 배치 내 중복 필터 ────────────────────

def _tokenize(title: str) -> set[str]:
    """제목을 토큰 집합으로 변환 (2글자 이상 단어)."""
    words = re.findall(r"[\w가-힣]{2,}", title)
    return set(words)

def _jaccard(a: str, b: str) -> float:
    sa, sb = _tokenize(a), _tokenize(b)
    if not sa or not sb:
        return 0.0
    return len(sa & sb) / len(sa | sb)

def deduplicate_by_title(articles: list[dict], threshold: float = 0.6) -> list[dict]:
    """
    같은 배치 내 제목 유사도가 threshold 이상인 기사를 제거한다.
    같은 이벤트를 여러 언론사가 보도한 경우를 탐지.

    Args:
        articles: 기사 목록
        threshold: Jaccard 유사도 임계값 (0.6 = 60% 토큰 겹침)

    Returns:
        중복 제거된 기사 목록 (앞에 나온 것 우선 유지)
    """
    kept: list[dict] = []
    for candidate in articles:
        title_c = candidate["title"]
        is_dup = any(
            _jaccard(title_c, kept_art["title"]) >= threshold
            for kept_art in kept
        )
        if is_dup:
            logger.debug("유사 제목 중복 제거: %s", title_c[:60])
        else:
            kept.append(candidate)
    removed = len(articles) - len(kept)
    if removed:
        logger.info("Jaccard 중복 제거: %d건 제외", removed)
    return kept


# ── CRUD ──────────────────────────────────────────────────

def upsert_news(session: Session, news_item: dict) -> Optional[SafetyNews]:
    """
    단건 뉴스를 DB에 저장한다.
    content_url 또는 title_hash 가 이미 존재하면 건너뛴다(L1+L2 중복 방지).
    """
    title_hash = make_title_hash(news_item["title"])

    record = SafetyNews(
        title=news_item["title"][:255],
        content_url=news_item.get("content_url"),
        title_hash=title_hash,
        published_at=news_item["published_at"],
        category_level=news_item["category_level"],
        region_name=news_item.get("region_name"),
        summary=news_item.get("summary"),
    )
    try:
        session.add(record)
        session.flush()
        logger.debug("저장: %s", record.title[:50])
        return record
    except IntegrityError:
        session.rollback()
        logger.debug(
            "중복 건너뜀 (URL 또는 제목 해시): %s",
            news_item.get("content_url", news_item["title"][:40]),
        )
        return None


def get_session_factory(engine):
    return sessionmaker(bind=engine, autoflush=False)
