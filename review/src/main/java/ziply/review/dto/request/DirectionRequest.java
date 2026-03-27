package ziply.review.dto.request;

public record DirectionRequest(
        Integer round,
        Double direction,
        String windowLocation
) {}