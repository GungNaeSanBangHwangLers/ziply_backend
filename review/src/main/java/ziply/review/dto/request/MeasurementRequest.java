package ziply.review.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MeasurementRequest(
        @Schema(description = "방향 각도 (0~360)", example = "180.5")
        Double direction,

        @Schema(description = "20회 측정된 실내 조도 리스트 (Lux)",
                example = "[1100.0, 1105.0, 1098.0, 1120.0, ...]")
        List<Double> lightLevel,

        @Schema(description = "측정 회차 (1회차, 2회차 등)", example = "1")
        Integer round
) {}