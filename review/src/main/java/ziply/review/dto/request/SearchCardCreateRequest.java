package ziply.review.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class SearchCardCreateRequest {

    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<BasePointRequest> basePoints;

    @Getter
    @NoArgsConstructor
    public static class BasePointRequest {
        private String alias;
        private String address;
        private Double latitude;
        private Double longitude;
    }
}