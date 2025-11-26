package ziply.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ziply.review.dto.request.SearchCardCreateRequest;
import ziply.review.service.SearchCardService;

@Tag(name = "Review - 주거 탐색 카드 API", description = "주거 탐색 카드 생성, 조회 및 기점 관리 API")
@RestController
@RequestMapping("/api/v1/review/card")
@RequiredArgsConstructor
public class SearchCardController {

    private final SearchCardService searchCardService;

    @Operation(summary = "주거 탐색 카드 생성", description = "새로운 주거 탐색 카드를 생성하고, 관련된 기점(학교, 회사 등) 정보를 함께 저장합니다.")
    @PostMapping
    public ResponseEntity<Long> createSearchCard(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @RequestBody SearchCardCreateRequest request
    ) {
        Long cardId = searchCardService.createSearchCard(userId, request);
        return ResponseEntity.ok(cardId);
    }
}