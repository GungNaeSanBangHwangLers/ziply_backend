package ziply.review.event;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseCreatedEvent {

    private Long houseId;
    private Double latitude;
    private Double longitude;

    private UUID searchCardId;

    private Long timestamp;
    private String action = "CREATED";

    private List<BasePointDetail> basePoints;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BasePointDetail {
        private Long id;
        private Double latitude;
        private Double longitude;
    }
}