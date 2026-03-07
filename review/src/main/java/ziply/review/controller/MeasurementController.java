package ziply.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ziply.review.dto.request.DirectionRequest;
import ziply.review.dto.request.LightLevelRequest;
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

    @Operation(summary = "이미지 업로드", description = "이미지만 별도로 여러 장 업로드합니다.")
    @PostMapping(value = "/{houseId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadImages(
            @PathVariable Long houseId,
            @RequestPart("images") List<MultipartFile> images,
            @AuthenticationPrincipal Long userId) {
        measurementService.uploadImages(userId, houseId, images);
        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "방향 데이터 저장/수정", description = "특정 회차의 방향 데이터를 저장합니다.")
    @PostMapping("/{houseId}/measure/direction")
    public ResponseEntity<Void> saveDirection(
            @PathVariable Long houseId,
            @RequestBody DirectionRequest request,
            @AuthenticationPrincipal Long userId) {
        measurementService.saveDirection(userId, houseId, request);
        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "채광 데이터 저장/수정", description = "특정 회차의 채광 데이터를 저장합니다.")
    @PostMapping("/{houseId}/measure/light")
    public ResponseEntity<Void> saveLightLevel(
            @PathVariable Long houseId,
            @RequestBody LightLevelRequest request,
            @AuthenticationPrincipal Long userId) {
        measurementService.saveLightLevel(userId, houseId, request);
        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "특정 회차(Round) 측정 데이터 삭제", description = "특정 회차만 삭제하고 뒷번호를 재정렬합니다.")
    @DeleteMapping("/{houseId}/measure/{round}")
    public ResponseEntity<Void> deleteRound(
            @PathVariable Long houseId,
            @PathVariable Integer round,
            @AuthenticationPrincipal Long userId) {
        measurementService.deleteMeasurementByRound(userId, houseId, round);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "하우스 측정 데이터 전체 삭제", description = "하우스의 모든 측정값과 이미지를 삭제합니다.")
    @DeleteMapping("/{houseId}")
    public ResponseEntity<Void> deleteMeasurement(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long houseId) {
        measurementService.deleteHouseMeasurement(userId, houseId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "하우스 측정 카드 데이터 조회")
    @GetMapping("/{houseId}/card")
    public ResponseEntity<List<MeasurementCardResponse>> getMeasurementCard(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long houseId) {
        return ResponseEntity.ok(measurementService.getMeasurementCardData(userId, houseId));
    }

    @Operation(summary = "탐색 카드별 평균 채광 점수 조회")
    @GetMapping("/score/{cardId}")
    public ResponseEntity<List<HouseSunlightResponse>> getCardAverageScore(
            @PathVariable UUID cardId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(measurementService.getHouseSunlightScoresByCard(cardId, userId));
    }

    @Operation(summary = "향별 하우스 그룹화 조회")
    @GetMapping("/card/{searchCardId}")
    public ResponseEntity<List<DirectionGroupResponse>> getDirectionGroups(
            @AuthenticationPrincipal Long userId,
            @PathVariable UUID searchCardId) {
        return ResponseEntity.ok(measurementService.getDirectionGroups(userId, searchCardId));
    }

    @Operation(summary = "하우스별 향 정보 조회")
    @GetMapping("/house/{houseId}")
    public ResponseEntity<List<DirectionGroupResponse>> getDirectionGroupsByHouse(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long houseId) {
        return ResponseEntity.ok(measurementService.getDirectionGroupsByHouse(userId, houseId));
    }
}