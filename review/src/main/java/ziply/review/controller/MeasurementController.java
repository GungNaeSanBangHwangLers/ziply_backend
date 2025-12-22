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
}