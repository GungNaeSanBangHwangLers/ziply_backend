package ziply.review.dto.request;

import java.util.List;

public record SunlightRequest(
        List<Double> lightLevels,
        String windowLocation
) {
}