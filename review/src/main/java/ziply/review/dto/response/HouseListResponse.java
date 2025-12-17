package ziply.review.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class HouseListResponse {
    private Long houseId;
    private String address;
    private LocalDateTime visitTime;
}