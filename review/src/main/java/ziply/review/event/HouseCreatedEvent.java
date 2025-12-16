package ziply.review.event;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseCreatedEvent {

    private Long houseId;
    private Double latitude;
    private Double longitude;
    private UUID searchCardId;
}