package ziply.analysis.event;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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