package ziply.review.dto.request;

import java.util.List;

public record LightLevelRequest(
        Integer round,
        List<Double> lightLevels
) {}