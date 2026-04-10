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
        // 방향, 채광, 사진이 모두 있는지 체크
        boolean hasDirection = house.getMeasurements().stream()
                .anyMatch(m -> m.getDirection() != null);
        
        boolean hasLightLevel = house.getMeasurements().stream()
                .anyMatch(m -> m.getLightLevel() != null);
        
        boolean hasImages = !house.getHouseImages().isEmpty();
        
        boolean isCompleted = hasDirection && hasLightLevel && hasImages;
        
        return HouseResponse.builder()
                .id(house.getId())
                .visitDateTime(house.getVisitDateTime())
                .address(house.getAddress())
                .isMeasurementCompleted(isCompleted)
                .build();
    }
}
