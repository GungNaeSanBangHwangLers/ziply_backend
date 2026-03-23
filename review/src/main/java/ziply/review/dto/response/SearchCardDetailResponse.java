package ziply.review.dto.response;

import java.time.LocalDate;
import java.util.List;

public record SearchCardDetailResponse(
        LocalDate date,
        boolean isAllCompleted,
        List<HouseResponse> houses
) {}