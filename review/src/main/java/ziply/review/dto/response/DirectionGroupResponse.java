package ziply.review.dto.response;

import java.util.List;

public record DirectionGroupResponse(
        String houseAlias,      // "A", "B", "C" ...
        Long houseId,
        List<WindowDirectionDetail> windows // 한 집의 여러 창문 측정 결과
) {
    public record WindowDirectionDetail(
            String windowLocation,  // "거실", "안방"
            String directionType,   // "남향"
            String features,        // "일조량이 풍부..."
            String pros,            // "겨울에 따뜻..."
            String cons             // "가구 변색 주의..."
    ) {}
}