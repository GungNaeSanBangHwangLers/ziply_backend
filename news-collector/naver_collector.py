"""
Ziply 치안 뉴스 수집기 - 네이버 뉴스 API 수집 모듈
구(25개) 단위 수집, 1년치 페이지네이션 지원
"""

import logging
import re
import time
from datetime import datetime, timedelta
from email.utils import parsedate_to_datetime
from html import unescape
from typing import Iterator

import httpx

from config import (
    NAVER_CLIENT_ID,
    NAVER_CLIENT_SECRET,
    NAVER_DISPLAY,
    NAVER_MAX_START,
    NAVER_NEWS_URL,
    NAVER_REQUEST_DELAY,
    NEWS_LOOKBACK_DAYS,
    SEARCH_KEYWORDS,
    SEOUL_DISTRICTS,
)

logger = logging.getLogger(__name__)

_HTML_TAG = re.compile(r"<[^>]+>")


def _strip_html(text: str) -> str:
    return unescape(_HTML_TAG.sub("", text)).strip()


def _parse_pub_date(rfc_date: str) -> datetime:
    try:
        return parsedate_to_datetime(rfc_date).replace(tzinfo=None)
    except Exception:
        return datetime.utcnow()


class NaverNewsClient:
    """네이버 검색 API를 통해 뉴스를 수집하는 클라이언트."""

    def __init__(self) -> None:
        if not NAVER_CLIENT_ID or not NAVER_CLIENT_SECRET:
            raise EnvironmentError(
                "NAVER_CLIENT_ID / NAVER_CLIENT_SECRET 환경변수가 설정되지 않았습니다."
            )
        self._headers = {
            "X-Naver-Client-Id": NAVER_CLIENT_ID,
            "X-Naver-Client-Secret": NAVER_CLIENT_SECRET,
        }
        self._cutoff_date = datetime.now() - timedelta(days=NEWS_LOOKBACK_DAYS)

    def _fetch_page(self, query: str, start: int) -> list[dict]:
        """단일 페이지 요청."""
        params = {
            "query": query,
            "display": NAVER_DISPLAY,
            "start": start,
            "sort": "date",
        }
        try:
            with httpx.Client(timeout=10.0) as client:
                resp = client.get(NAVER_NEWS_URL, headers=self._headers, params=params)
                resp.raise_for_status()
                return resp.json().get("items", [])
        except httpx.HTTPStatusError as e:
            logger.warning("API 오류 [%s] query=%s start=%d: %s", e.response.status_code, query, start, e)
            return []
        except Exception as e:
            logger.warning("요청 실패 query=%s start=%d: %s", query, start, e)
            return []

    def _collect_region(self, region_name: str) -> Iterator[dict]:
        """
        지역 이름(구)으로 1년치 뉴스를 페이지네이션하며 수집한다.
        날짜 순(최신→과거)으로 수집하며, cutoff_date 이전 기사가 나오면 중단한다.
        """
        seen_urls: set[str] = set()

        for keyword_template in SEARCH_KEYWORDS:
            query = keyword_template.format(district=region_name)
            logger.debug("검색: %s", query)

            start = 1
            while start <= NAVER_MAX_START:
                items = self._fetch_page(query, start)
                if not items:
                    break

                reached_cutoff = False
                for item in items:
                    pub_date = _parse_pub_date(item.get("pubDate", ""))

                    # 1년 이전 기사가 나오면 이 쿼리는 중단
                    if pub_date < self._cutoff_date:
                        reached_cutoff = True
                        break

                    url = item.get("originallink") or item.get("link", "")
                    if not url or url in seen_urls:
                        continue
                    seen_urls.add(url)

                    yield {
                        "title": _strip_html(item.get("title", "")),
                        "content_url": url,
                        "published_at": pub_date,
                        "description": _strip_html(item.get("description", "")),
                        "region_name": region_name,
                    }

                if reached_cutoff or len(items) < NAVER_DISPLAY:
                    break

                start += NAVER_DISPLAY
                time.sleep(NAVER_REQUEST_DELAY)

            time.sleep(NAVER_REQUEST_DELAY)

    def collect_district(self, district: str) -> Iterator[dict]:
        """특정 구 단위로 1년치 뉴스를 수집한다."""
        yield from self._collect_region(district)

    def collect_all_districts(self) -> Iterator[tuple[str, dict]]:
        """서울 25개 구 전체를 순회하며 뉴스를 수집한다."""
        for district in SEOUL_DISTRICTS:
            logger.info("[구] 수집 시작: %s", district)
            for article in self.collect_district(district):
                yield district, article
