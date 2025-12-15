package ziply.review.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SearchCardCreateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startDate;

    private LocalDate endDate;

    @Valid
    private List<BasePointRequest> basePoints;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class BasePointRequest {
        @NotBlank(message = "기점 별칭은 필수입니다.")
        private String alias;

        @NotBlank(message = "주소는 필수입니다.")
        private String address;

        @NotNull(message = "위도는 필수입니다.")
        private Double latitude;

        @NotNull(message = "경도는 필수입니다.")
        private Double longitude;
    }
}