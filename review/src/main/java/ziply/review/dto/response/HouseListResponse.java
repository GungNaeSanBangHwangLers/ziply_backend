package ziply.review.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class HouseListResponse {
    private Long houseId;
    private String label;
    private String address;
    private LocalDateTime visitTime;

    private List<String> imageUrls;
}