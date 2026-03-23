package ziply.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ziply.review.dto.request.SearchCardCreateRequest;
import ziply.review.dto.response.BasePointAddressResponse;
import ziply.review.dto.response.MapInfoResponse;
import ziply.review.dto.response.SearchCardDetailResponse;
import ziply.review.dto.response.SearchCardResponse;
import ziply.review.service.SearchCardService;

@Tag(name = "Review - 주거 탐색 카드 API", description = "주거 탐색 카드 생성, 조회 및 기점 관리 API")
@RestController
@RequestMapping("/api/v1/review/card")
@RequiredArgsConstructor
public class SearchCardController {

    private final SearchCardService searchCardService;

    @Operation(
            summary = "지도 정보 조회",
            description = "특정 카드의 지도 관련 명칭, 위도, 경도를 조회합니다."
    )
    @GetMapping("/map/{searchCardId}")
    public ResponseEntity<List<MapInfoResponse>> getMapInfo(
            @PathVariable UUID searchCardId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        List<MapInfoResponse> responses = searchCardService.getMapInfo(searchCardId, userId);
        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "기점 주소 정보 조회",
            description = "특정 카드의 모든 기점(목적지) 명칭과 주소를 조회합니다."
    )
    @GetMapping("/{searchCardId}/addresses")
    public ResponseEntity<List<BasePointAddressResponse>> getBasePointAddresses(
            @PathVariable UUID searchCardId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        List<BasePointAddressResponse> responses = searchCardService.getBasePointAddresses(searchCardId, userId);

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "주거 탐색 카드 전체 조회", description = "사용자의 주거 탐색 카드를 조회하고, 불러옵니다.")
    @GetMapping
    public ResponseEntity<List<SearchCardResponse>> readSearchCard(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        List<SearchCardResponse> responseList = searchCardService.getSearchCards(userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseList);
    }

    @Operation(summary = "주거 탐색 카드 생성", description = "새로운 주거 탐색 카드를 생성 및 관련된 기점, 집들의 정보를 함께 저장합니다.")
    @PostMapping
    public ResponseEntity<UUID> createSearchCard(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                                 @RequestBody @Valid SearchCardCreateRequest request) {
        UUID cardId = searchCardService.createSearchCard(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cardId);
    }

    @Operation(
            summary = "주거 탐색 카드 상세 조회 (정렬 포함)",
            description = "날짜별로 그룹화되고, 완료 여부 및 시간에 따라 정렬된 주거 목록을 조회합니다."
    )
    @GetMapping("/{searchCardId}/details")
    public ResponseEntity<List<SearchCardDetailResponse>> getSearchCardDetails(
            @PathVariable UUID searchCardId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        List<SearchCardDetailResponse> responses = searchCardService.getSortedCardDetails(searchCardId, userId);

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{searchCardId}/owner-check")
    public ResponseEntity<Boolean> checkOwner(@PathVariable UUID searchCardId, @RequestParam Long userId) {
        boolean isOwner = searchCardService.isCardOwner(searchCardId, userId);
        return ResponseEntity.ok(isOwner);
    }
}