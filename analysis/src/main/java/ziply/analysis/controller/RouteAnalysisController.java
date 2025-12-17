package ziply.analysis.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ziply.analysis.dto.response.SearchCardAnalysisResponse;
import ziply.analysis.service.RouteAnalysisService;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class RouteAnalysisController {

    private final RouteAnalysisService routeAnalysisService;

    @GetMapping("/{searchCardId}")
    public ResponseEntity<SearchCardAnalysisResponse> getSearchCardAnalysis(@PathVariable UUID searchCardId,
                                                                            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(routeAnalysisService.getAnalysisByCard(searchCardId, userId));
    }
}