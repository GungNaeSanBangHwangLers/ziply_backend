package ziply.review.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record MeasurementRequest(
        @Schema(description = "방향 각도 (0~360)", example = "180.5")
        Double direction,

        @Schema(description = "채광 측정 수치", example = "450.0")
        Double lightLevel
) {}