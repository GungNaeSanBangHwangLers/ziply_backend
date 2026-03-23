package ziply.review.dto.response;

import lombok.Builder;
import ziply.review.domain.House;
import ziply.review.domain.HouseStatus;

@Builder
public record HouseResponse(
        Long id,
        String address,
        boolean isMeasurementCompleted
) {
    public static HouseResponse from(House house) {
        return HouseResponse.builder()
                .id(house.getId())
                .address(house.getAddress())
                .isMeasurementCompleted(!house.getMeasurements().isEmpty() || house.getStatus() == HouseStatus.AFTER)
                .build();
    }
}