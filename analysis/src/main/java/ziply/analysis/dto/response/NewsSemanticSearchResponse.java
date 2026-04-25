package ziply.analysis.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GET /api/v1/analysis/news/search?query=...&regionName=...
 * Qdrant 시맨틱 검색 결과 DTO.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsSemanticSearchResponse {

    private String query;
    private int totalCount;
    private List<SearchResult> results;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        private Long newsId;
        private String title;
        private String regionName;
        private Integer categoryLevel;
        private String publishedAt;
        private String contentUrl;
        private String summary;
        private double score;        // 유사도 점수 (0~1, 높을수록 유사)
    }
}
