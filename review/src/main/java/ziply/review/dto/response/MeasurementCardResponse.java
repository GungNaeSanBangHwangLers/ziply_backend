package ziply.review.dto.response;

import java.util.List;

public record MeasurementCardResponse(
        Integer round,
        String title,
        boolean isDirectionDone,
        boolean isLightDone,
        String directionStatus,
        String lightStatus,
        Double direction,
        Double lightLevel,
        List<String> imageUrls
) {
}