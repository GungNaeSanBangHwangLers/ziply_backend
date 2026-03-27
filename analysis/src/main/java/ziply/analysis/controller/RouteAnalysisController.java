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
import ziply.analysis.service.RouteAnalysisService;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class RouteAnalysisController {

    private final RouteAnalysisService routeAnalysisService;

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
}