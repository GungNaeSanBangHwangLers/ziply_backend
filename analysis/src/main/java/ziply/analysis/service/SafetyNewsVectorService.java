package ziply.analysis.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import ziply.analysis.dto.response.NewsSemanticSearchResponse;
import ziply.analysis.dto.response.NewsSemanticSearchResponse.SearchResult;

/**
 * Qdrant 벡터 DB를 활용한 치안 뉴스 시맨틱 검색 서비스.
 *
 * 데이터 적재: Python news-collector/qdrant_handler.py
 * 컬렉션명: safety-news (application.yml 설정과 동일)
 * 벡터: text-embedding-3-small (1536차원)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SafetyNewsVectorService {

    private final VectorStore vectorStore;

    private static final int DEFAULT_TOP_K = 10;

    /**
     * 자연어 쿼리로 치안 뉴스를 시맨틱 검색한다.
     *
     * @param query      검색 쿼리 (예: "밤에 혼자 다니기 위험한 동네", "절도 사건")
     * @param regionName 지역 필터 (예: "상도동", null이면 전체)
     * @param topK       반환 건수 (기본 10)
     */
    public NewsSemanticSearchResponse search(String query, String regionName, int topK) {
        log.info("[VectorSearch] query='{}', region={}, topK={}", query, regionName, topK);

        SearchRequest.Builder requestBuilder = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.5);   // 유사도 0.5 미만 제외

        // 지역 필터 적용
        if (regionName != null && !regionName.isBlank()) {
            FilterExpressionBuilder fb = new FilterExpressionBuilder();
            requestBuilder.filterExpression(
                    fb.eq("region_name", regionName).build()
            );
        }

        List<Document> docs = vectorStore.similaritySearch(requestBuilder.build());

        List<SearchResult> results = docs.stream()
                .map(this::toSearchResult)
                .collect(Collectors.toList());

        return NewsSemanticSearchResponse.builder()
                .query(query)
                .totalCount(results.size())
                .results(results)
                .build();
    }

    /**
     * 특정 집 주소(regionName) 기반으로 위험 관련 뉴스를 시맨틱 검색.
     * 예: "이 동네 위험한 사건 사고" 쿼리 + regionName 필터
     */
    public NewsSemanticSearchResponse searchDangerousNews(String regionName, int topK) {
        String query = regionName + " 위험 사건 범죄 신변위협 주거침입 강도";
        return search(query, regionName, topK);
    }

    private SearchResult toSearchResult(Document doc) {
        Map<String, Object> meta = doc.getMetadata();

        Object newsIdRaw = meta.get("news_id");
        Long newsId = newsIdRaw instanceof Number n ? n.longValue() : null;

        Object levelRaw = meta.get("category_level");
        Integer categoryLevel = levelRaw instanceof Number n ? n.intValue() : null;

        // Spring AI Document의 score는 getScore()로 접근
        double score = doc.getScore() != null ? doc.getScore() : 0.0;

        return SearchResult.builder()
                .newsId(newsId)
                .title((String) meta.getOrDefault("title", ""))
                .regionName((String) meta.getOrDefault("region_name", ""))
                .categoryLevel(categoryLevel)
                .publishedAt((String) meta.getOrDefault("published_at", ""))
                .contentUrl((String) meta.getOrDefault("content_url", ""))
                .summary((String) meta.getOrDefault("summary", ""))
                .score(score)
                .build();
    }
}
