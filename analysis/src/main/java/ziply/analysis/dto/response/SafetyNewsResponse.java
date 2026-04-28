package ziply.analysis.dto.response;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GET /api/v1/analysis/news/{searchCardId}?period=3&level=2&page=0&size=10 응답 DTO.
 * level 파라미터로 레벨별 뉴스를 독립적으로 조회.
 * level 생략 시 카운트/메시지만 반환 (탭 배지용).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetyNewsResponse {

    private String label;           // A, B, C ...
    private String regionName;      // 법정동 이름 (예: 상도동)

    private int period;              // 조회 기간 (개월 수)

    private int level1Count;        // 생활 불편
    private int level2Count;        // 안전 불안
    private int level3Count;        // 신변 위협
    private int totalNewsCount;     // 전체 레벨 합산 건수 (level1+level2+level3)

    private List<NewsItem> news;    // 요청한 레벨의 기사 목록

    private int page;               // 현재 페이지 (0-based)
    private int size;               // 페이지당 항목 수
    private long totalCount;        // 해당 레벨 전체 기사 수
    private int totalPages;         // 해당 레벨 전체 페이지 수

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewsItem {
        private String title;
        private String categoryLevel;   // "생활 불편 / 무질서" | "안전 불안 / 재산 위협" | "신변 위협 / 강력 범죄"
        private String categoryTag;     // 중분류 (예: "재산 범죄", "대인 강력")
        private LocalDate publishedAt;
        private String contentUrl;
        private String summary;
    }
}
