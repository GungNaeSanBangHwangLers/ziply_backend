package ziply.analysis.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GET /api/v1/analysis/news/{searchCardId}?period=3 응답 DTO.
 * 탐색카드 내 각 집(houseId)의 동별 치안 뉴스 집계 결과.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetyNewsResponse {

    private Long houseId;
    private String label;          // A, B, C ...
    private String regionName;     // 법정동 이름 (예: 상도동)
    private int period;            // 조회 기간 (개월)

    private int level1Count;       // 생활 불편
    private int level2Count;       // 안전 불안
    private int level3Count;       // 신변 위협
    private int totalCount;

    private String message;        // 요약 메시지

    private List<NewsItem> recentNews;   // 최신 기사 목록 (최대 5건)

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewsItem {
        private Long id;
        private String title;
        private int categoryLevel;
        private LocalDateTime publishedAt;
        private String contentUrl;
        private String summary;
    }
}
