"""
Ziply 치안 뉴스 수집기 - Qdrant 벡터 DB 저장 모듈

MySQL에 저장된 뉴스를 OpenAI text-embedding-3-small로 임베딩하여
Qdrant safety-news 컬렉션에 upsert한다.
"""

import logging
import os
import uuid
from datetime import datetime
from typing import Optional

from openai import OpenAI
from qdrant_client import QdrantClient
from qdrant_client.models import (
    Distance,
    PointStruct,
    VectorParams,
    PayloadSchemaType,
)

from config import OPENAI_API_KEY

logger = logging.getLogger(__name__)

# ── 설정 ──────────────────────────────────────────────────
QDRANT_HOST = os.getenv("QDRANT_HOST", "localhost")
QDRANT_PORT = int(os.getenv("QDRANT_PORT", "6333"))   # REST port
COLLECTION_NAME = "safety-news"                        # Java application.yml과 동일
EMBEDDING_MODEL = "text-embedding-3-small"
VECTOR_SIZE = 1536


class QdrantNewsHandler:
    """
    치안 뉴스를 임베딩하여 Qdrant에 저장하는 핸들러.

    Point ID: MySQL safety_news.id (int → uuid 변환 없이 uint64 그대로 사용)
    Vector: title + summary 결합 텍스트의 임베딩
    Payload: 전체 뉴스 메타데이터 (MySQL과 동일한 필드)
    """

    def __init__(self) -> None:
        self._openai = OpenAI(api_key=OPENAI_API_KEY)
        self._qdrant = QdrantClient(host=QDRANT_HOST, port=QDRANT_PORT)
        self._ensure_collection()

    def _ensure_collection(self) -> None:
        """safety-news 컬렉션이 없으면 생성한다."""
        existing = [c.name for c in self._qdrant.get_collections().collections]
        if COLLECTION_NAME not in existing:
            self._qdrant.create_collection(
                collection_name=COLLECTION_NAME,
                vectors_config=VectorParams(
                    size=VECTOR_SIZE,
                    distance=Distance.COSINE,
                ),
            )
            # 페이로드 인덱스 생성 (필터 검색 성능 향상)
            self._qdrant.create_payload_index(COLLECTION_NAME, "region_name", PayloadSchemaType.KEYWORD)
            self._qdrant.create_payload_index(COLLECTION_NAME, "category_level", PayloadSchemaType.INTEGER)
            self._qdrant.create_payload_index(COLLECTION_NAME, "published_at", PayloadSchemaType.KEYWORD)
            logger.info("Qdrant 컬렉션 생성 완료: %s", COLLECTION_NAME)
        else:
            logger.debug("Qdrant 컬렉션 이미 존재: %s", COLLECTION_NAME)

    def _embed(self, text: str) -> list[float]:
        """텍스트를 OpenAI 임베딩 벡터로 변환한다."""
        response = self._openai.embeddings.create(
            model=EMBEDDING_MODEL,
            input=text[:8000],   # 토큰 제한 여유분
        )
        return response.data[0].embedding

    def _build_embed_text(self, news_item: dict) -> str:
        """임베딩할 텍스트 조합: 제목 + 요약 + 지역명"""
        parts = [news_item.get("title", "")]
        if news_item.get("summary"):
            parts.append(news_item["summary"])
        if news_item.get("region_name"):
            parts.append(news_item["region_name"])
        return " | ".join(filter(None, parts))

    def upsert(self, news_item: dict, mysql_id: int) -> bool:
        """
        단건 뉴스를 Qdrant에 upsert한다.

        Args:
            news_item: {
                "title": str,
                "content_url": str,
                "published_at": datetime,
                "category_level": int,
                "region_name": str,
                "summary": str | None,
            }
            mysql_id: MySQL safety_news.id (Point ID로 사용)

        Returns:
            True if success
        """
        try:
            embed_text = self._build_embed_text(news_item)
            vector = self._embed(embed_text)

            published_at = news_item.get("published_at")
            published_str = (
                published_at.isoformat()
                if isinstance(published_at, datetime)
                else str(published_at)
            )

            point = PointStruct(
                id=mysql_id,
                vector=vector,
                payload={
                    "news_id": mysql_id,
                    "title": news_item.get("title", ""),
                    "content_url": news_item.get("content_url", ""),
                    "published_at": published_str,
                    "category_level": news_item.get("category_level"),
                    "region_name": news_item.get("region_name", ""),
                    "summary": news_item.get("summary", ""),
                },
            )

            self._qdrant.upsert(collection_name=COLLECTION_NAME, points=[point])
            logger.debug("Qdrant upsert 완료: id=%d, region=%s", mysql_id, news_item.get("region_name"))
            return True

        except Exception as e:
            logger.warning("Qdrant upsert 실패 id=%d: %s", mysql_id, e)
            return False

    # OpenAI Embeddings API 및 Qdrant 단일 요청 한도
    _EMBED_CHUNK = 500

    def bulk_upsert(self, news_items: list[dict], mysql_ids: list[int]) -> int:
        """
        여러 뉴스를 청크 단위로 나눠 Qdrant에 저장한다.
        OpenAI Embeddings API / Qdrant 요청 한도(_EMBED_CHUNK)를 초과하지 않도록 분할.

        Returns:
            성공한 건수
        """
        if not news_items:
            return 0

        total = 0
        for i in range(0, len(news_items), self._EMBED_CHUNK):
            chunk_items = news_items[i : i + self._EMBED_CHUNK]
            chunk_ids   = mysql_ids[i : i + self._EMBED_CHUNK]
            total += self._upsert_chunk(chunk_items, chunk_ids)

        logger.info("Qdrant 배치 upsert 완료: %d건", total)
        return total

    def _upsert_chunk(self, news_items: list[dict], mysql_ids: list[int]) -> int:
        try:
            texts = [self._build_embed_text(item) for item in news_items]
            response = self._openai.embeddings.create(
                model=EMBEDDING_MODEL,
                input=[t[:8000] for t in texts],
            )
            vectors = [r.embedding for r in response.data]

            points = []
            for item, mysql_id, vector in zip(news_items, mysql_ids, vectors):
                published_at = item.get("published_at")
                published_str = (
                    published_at.isoformat()
                    if isinstance(published_at, datetime)
                    else str(published_at)
                )
                points.append(PointStruct(
                    id=mysql_id,
                    vector=vector,
                    payload={
                        "news_id": mysql_id,
                        "title": item.get("title", ""),
                        "content_url": item.get("content_url", ""),
                        "published_at": published_str,
                        "category_level": item.get("category_level"),
                        "category_tag": item.get("category_tag"),
                        "region_name": item.get("region_name", ""),
                        "summary": item.get("summary", ""),
                    },
                ))

            self._qdrant.upsert(collection_name=COLLECTION_NAME, points=points)
            return len(points)

        except Exception as e:
            logger.error("Qdrant 청크 upsert 실패: %s", e)
            return 0

    def search(self, query: str, region_name: Optional[str] = None,
               category_level: Optional[int] = None, limit: int = 10) -> list[dict]:
        """
        시맨틱 검색 (추후 Java 서비스 외 직접 활용 시 사용).

        Args:
            query: 검색 쿼리 텍스트
            region_name: 지역 필터 (None이면 전체)
            category_level: 레벨 필터 (None이면 전체)
            limit: 반환 건수
        """
        vector = self._embed(query)

        qdrant_filter = None
        conditions = []
        if region_name:
            conditions.append({"key": "region_name", "match": {"value": region_name}})
        if category_level:
            conditions.append({"key": "category_level", "match": {"value": category_level}})
        if conditions:
            qdrant_filter = {"must": conditions}

        results = self._qdrant.search(
            collection_name=COLLECTION_NAME,
            query_vector=vector,
            query_filter=qdrant_filter,
            limit=limit,
            with_payload=True,
        )
        return [{"score": r.score, **r.payload} for r in results]
