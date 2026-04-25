package ziply.review.event;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class HouseUpdatedEvent {

    private Long houseId;
    private UUID searchCardId;

    private String address;
    private String regionName;

    private Double latitude;
    private Double longitude;

    private Long timestamp;

    @Builder.Default
    private String action = "UPDATED";

    private List<BasePointDetail> basePoints;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class BasePointDetail {
        private Long id;
        private String name;
        private Double latitude;
        private Double longitude;
    }
}
