package ziply.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ziply.review.dto.request.MeasurementRequest;
import ziply.review.service.MeasurementService;



@Tag(name = "Measurement API", description = "하우스 측정 데이터 관리 API")
@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
public class MeasurementController {

    private final MeasurementService measurementService;

    @Operation(summary = "하우스 측정 데이터 저장 (측정하기)",
            description = "방향(0~360도)과 채광 수치를 저장하고, 하우스 상태를 '탐색후'로 변경합니다.")
    @PostMapping("/house/{houseId}/measure")
    public ResponseEntity<Void> addMeasurement(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long houseId,
            @RequestBody List<MeasurementRequest> request) {

        measurementService.addBulkMeasurements(userId, houseId, request);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "하우스 측정 카드 데이터 조회", description = "이미지처럼 1~3차 방향/채광 측정 상태를 반환합니다.")
    @GetMapping("/house/{houseId}/card")
    public ResponseEntity<List<MeasurementCardResponse>> getMeasurementCard(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long houseId) {

        // 서비스에서 가공된 DTO 리스트를 받아옴
        List<MeasurementCardResponse> response = measurementService.getMeasurementCardData(userId, houseId);
        return ResponseEntity.ok(response);
    }

    // --- 응답 DTO ---
    public record MeasurementCardResponse(
            Integer round,
            String title,           // "1차 측정"
            boolean isDirectionDone,
            boolean isLightDone,
            String directionStatus, // "방향 측정 완료" or "미완료"
            String lightStatus,     // "채광 측정 완료" or "미완료"
            Double direction,       // 실제 각도값 (필요시)
            Double lightLevel       // 실제 채광값 (필요시)
    ) {}
}