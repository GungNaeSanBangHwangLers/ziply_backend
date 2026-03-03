package ziply.review.dto.request;

import java.util.List;

public record CombinedMeasurementRequest(
        Double direction,      // 방향 각도
        List<Double> lightLevels, // 조도 리스트
        String windowLocation  // 창문 위치
) {}