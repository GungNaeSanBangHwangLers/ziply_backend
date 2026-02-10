package ziply.review.dto.response;

public record HouseSunlightResponse(
        Long houseId,
        String label,
        Integer score
) {}