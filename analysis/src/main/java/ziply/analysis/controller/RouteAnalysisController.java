package ziply.analysis.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ziply.analysis.dto.response.BasePointAnalysisDto;
import ziply.analysis.dto.response.LifeScoreAnalysisResponse;
import ziply.analysis.dto.response.SafetyAnalysisResponse;
import ziply.analysis.dto.response.NewsSemanticSearchResponse;
import ziply.analysis.dto.response.SafetyNewsResponse;
import ziply.analysis.service.RouteAnalysisService;
import ziply.analysis.service.SafetyNewsService;
import ziply.analysis.service.SafetyNewsVectorService;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class RouteAnalysisController {

    private final RouteAnalysisService routeAnalysisService;
    private final SafetyNewsService safetyNewsService;
    private final SafetyNewsVectorService safetyNewsVectorService;

    @GetMapping("/distance/{searchCardId}")
    public ResponseEntity<List<BasePointAnalysisDto>> getSearchCardDistanceAnalysis(
            @PathVariable UUID searchCardId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(routeAnalysisService.getSearchCardDistanceAnalysis(searchCardId, userId));
    }

    @GetMapping("/life/{searchCardId}")
    public ResponseEntity<List<LifeScoreAnalysisResponse>> getLifeScoreAnalysis(
            @PathVariable UUID searchCardId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(routeAnalysisService.getLifeScoreAnalysis(searchCardId, userId));
    }

    @GetMapping("/safety/{searchCardId}")
    public ResponseEntity<List<SafetyAnalysisResponse>> getSafetyAnalysis(
            @PathVariable UUID searchCardId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(routeAnalysisService.getSafetyAnalysis(searchCardId, userId));
    }

    /**
     * 탐색카드 내 각 집의 동(법정동) 기반 치안 뉴스 집계 조회.
     *
     * @param period 조회 기간 (개월): 3, 6, 12 (기본값 3)
     * @param page   페이지 번호 (0-based, 기본값 0)
     * @param size   페이지당 항목 수 (기본값 10, 최대 50)
     */
    @GetMapping("/news/{searchCardId}")
    public ResponseEntity<List<SafetyNewsResponse>> getSafetyNews(
            @PathVariable UUID searchCardId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "3") int period,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (period != 3 && period != 6 && period != 12) {
            return ResponseEntity.badRequest().build();
        }
        if (size < 1 || size > 50) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(safetyNewsService.getNewsAnalysis(searchCardId, userId, period, page, size));
    }

    /**
     * 자연어 쿼리로 치안 뉴스 시맨틱 검색 (Qdrant).
     *
     * @param query      검색어 (예: "밤에 위험한 사건", "절도")
     * @param regionName 지역 필터 (예: 상도동, 생략 시 전체)
     * @param topK       반환 건수 (기본 10)
     */
    @GetMapping("/news/search")
    public ResponseEntity<NewsSemanticSearchResponse> searchNewsSemantic(
            @RequestParam String query,
            @RequestParam(required = false) String regionName,
            @RequestParam(defaultValue = "10") int topK) {
        return ResponseEntity.ok(
                safetyNewsVectorService.search(query, regionName, topK)
        );
    }
}