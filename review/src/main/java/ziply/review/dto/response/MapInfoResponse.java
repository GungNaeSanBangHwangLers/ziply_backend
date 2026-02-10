package ziply.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapInfoResponse {
    private Long id;
    private String label;
    private Double latitude;
    private Double longitude;
    private String address;
}