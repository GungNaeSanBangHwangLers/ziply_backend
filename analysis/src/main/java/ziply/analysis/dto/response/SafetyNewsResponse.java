package ziply.analysis.dto.response;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GET /api/v1/analysis/news/{searchCardId}?period=3&page=0&size=10 응답 DTO.
 * 탐색카드 내 각 집(houseId)의 동별 치안 뉴스 집계 결과 (페이지네이션 포함).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetyNewsResponse {

    private String label;           // A, B, C ...
    private String regionName;      // 법정동 이름 (예: 상도동)

    private int level1Count;        // 생활 불편
    private int level2Count;        // 안전 불안
    private int level3Count;        // 신변 위협

    private String message;         // 요약 메시지

    private List<NewsItem> recentNews;   // 페이지네이션된 기사 목록

    private int page;               // 현재 페이지 (0-based)
    private int size;               // 페이지당 항목 수
    private long totalCount;        // 전체 기사 수
    private int totalPages;         // 전체 페이지 수

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewsItem {
        private String title;
        private String categoryLevel;   // "생활 불편 / 무질서" | "안전 불안 / 재산 위협" | "신변 위협 / 강력 범죄"
        private String categoryTag;     // 중분류 (예: "재산 범죄", "대인 강력") — 재분류 전 null
        private LocalDate publishedAt;
        private String contentUrl;
        private String summary;
    }
}
