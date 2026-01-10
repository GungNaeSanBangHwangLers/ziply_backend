package ziply.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ziply.review.dto.request.MeasurementRequest;
import ziply.review.dto.response.DirectionGroupResponse;
import ziply.review.dto.response.HouseSunlightResponse;
import ziply.review.dto.response.MeasurementCardResponse;
import ziply.review.service.MeasurementService;


@Tag(name = "Measurement API", description = "하우스 측정 데이터 관리 API")
@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
public class MeasurementController {

    private final MeasurementService measurementService;

    @Operation(summary = "탐색 카드별 평균 채광 점수 조회", description = "특정 탐색 카드에 속한 모든 하우스의 채광 측정값 평균 점수를 반환합니다.")
    @GetMapping("/score/{cardId}")
    public ResponseEntity<List<HouseSunlightResponse>> getCardAverageScore(
            @PathVariable UUID cardId,
            @AuthenticationPrincipal Long userId) {

        List<HouseSunlightResponse> response = measurementService.getHouseSunlightScoresByCard(cardId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "하우스 측정 데이터 저장 (측정하기)", description = "방향(0~360도)과 채광 수치를 저장하고, 하우스 상태를 '탐색후'로 변경합니다.")
    @PostMapping("/{houseId}/measure")
    public ResponseEntity<Void> addMeasurement(@AuthenticationPrincipal Long userId, @PathVariable Long houseId,
                                               @RequestBody List<MeasurementRequest> request) {

        measurementService.addBulkMeasurements(userId, houseId, request);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "하우스 측정 카드 데이터 조회", description = "이미지처럼 1~3차 방향/채광 측정 상태를 반환합니다.")
    @GetMapping("/{houseId}/card")
    public ResponseEntity<List<MeasurementCardResponse>> getMeasurementCard(@AuthenticationPrincipal Long userId,
                                                                            @PathVariable Long houseId) {
        List<MeasurementCardResponse> response = measurementService.getMeasurementCardData(userId, houseId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "하우스 측정 데이터 전체 다시 측정 (업데이트)", description = "1~3차 측정 데이터를 리스트로 받아 전체 수정합니다.")
    @PatchMapping("/{houseId}/measure")
    public ResponseEntity<Void> reMeasureAll(@AuthenticationPrincipal Long userId, @PathVariable Long houseId,
                                             @RequestBody List<MeasurementRequest> requests) {

        measurementService.reMeasure(userId, houseId, requests);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "특정 차수 데이터 삭제", description = "지정한 회차(round)의 데이터만 삭제합니다. 순서는 유지됩니다.")
    @DeleteMapping("/{houseId}/measure/{round}")
    public ResponseEntity<Void> deleteMeasurement(@AuthenticationPrincipal Long userId, @PathVariable Long houseId,
                                                  @PathVariable Integer round) {
        measurementService.deleteMeasurement(userId, houseId, round);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "향별 하우스 그룹화 조회", description = "특정 주거탐색카드 내의 하우스들을 향별로 묶어 하우스 ID 리스트와 해당 향의 특징/장단점을 반환합니다.")
    @GetMapping("/card/{searchCardId}")
    public ResponseEntity<List<DirectionGroupResponse>> getDirectionGroups(@AuthenticationPrincipal Long userId,
                                                                           @PathVariable UUID searchCardId) {
        return ResponseEntity.ok(measurementService.getDirectionGroups(userId, searchCardId));
    }

    @Operation(summary = "하우스별 향 정보 조회", description = "특정 하우스 ID를 기반으로 해당 집의 향 정보와 특징/장단점을 반환합니다.")
    @GetMapping("/house/{houseId}")
    public ResponseEntity<List<DirectionGroupResponse>> getDirectionGroupsByHouse(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long houseId) {

        return ResponseEntity.ok(measurementService.getDirectionGroupsByHouse(userId, houseId));
    }
}