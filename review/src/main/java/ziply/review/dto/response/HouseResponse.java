package ziply.review.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Builder;
import ziply.review.domain.House;
import ziply.review.domain.HouseStatus;

@Builder
public record HouseResponse(
        Long id,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm", timezone = "Asia/Seoul")
        LocalDateTime visitDateTime, // "18:30"
        String address,
        boolean isMeasurementCompleted
) {
    public static HouseResponse from(House house) {
        return HouseResponse.builder()
                .id(house.getId())
                .visitDateTime(house.getVisitDateTime())
                .address(house.getAddress())
                .isMeasurementCompleted(!house.getMeasurements().isEmpty() || house.getStatus() == HouseStatus.AFTER)
                .build();
    }
}