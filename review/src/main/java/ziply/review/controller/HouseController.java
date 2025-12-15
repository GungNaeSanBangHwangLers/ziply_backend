package ziply.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ziply.review.dto.request.HouseCreateRequest;
import ziply.review.service.HouseService;

import java.util.UUID;

@Tag(name = "Review - 주거 탐색 집(매물) API", description = "주거 탐색 카드 내 집(매물) 추가 및 관리 API")
@RestController
@RequestMapping("/api/v1/review/card")
@RequiredArgsConstructor
public class HouseController {

    private final HouseService houseService;

    @Operation(summary = "집(매물) 일괄 추가", description = "여러 개의 집 정보를 리스트로 받아 한 번에 저장합니다.")
    @PostMapping("/{cardId}/houses")
    public ResponseEntity<List<Long>> createHouses(
            @PathVariable UUID cardId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid List<HouseCreateRequest> requests
    ) {
        List<Long> createdIds = houseService.createHouses(cardId, requests, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdIds);
    }
}
