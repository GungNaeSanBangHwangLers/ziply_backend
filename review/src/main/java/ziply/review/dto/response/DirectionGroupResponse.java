package ziply.review.dto.response;

import java.util.List;

public record DirectionGroupResponse(
        String directionType,      // "남향"
        String features,           // DB의 directionFeatures
        String pros,               // DB의 directionPros
        String cons,               // DB의 directionCons
        List<Long> houseIds        // 해당 향을 가진 하우스 ID 리스트 (중복 제거)
) {}