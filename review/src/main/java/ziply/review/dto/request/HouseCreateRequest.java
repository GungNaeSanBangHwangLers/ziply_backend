package ziply.review.dto.request;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HouseCreateRequest {
    private String address;
    private LocalDateTime visitDateTime;
    private Double latitude;
    private Double longitude;
}