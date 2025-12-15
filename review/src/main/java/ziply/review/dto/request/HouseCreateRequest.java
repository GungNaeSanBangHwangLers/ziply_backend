package ziply.review.dto.request;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class HouseCreateRequest {
    private String address;
    private LocalDateTime visitDateTime;
}