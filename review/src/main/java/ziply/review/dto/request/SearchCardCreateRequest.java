package ziply.review.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
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

    @Valid
    @Size(max = 7, message = "집은 한 번에 최대 7개까지 등록할 수 있습니다.")
    private List<HouseCreateRequest> houses;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class BasePointRequest {
        @NotBlank(message = "기점 별칭은 필수입니다.")
        private String alias;
        @NotBlank(message = "주소는 필수입니다.")
        private String address;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class HouseCreateRequest {
        @NotBlank(message = "집 주소는 필수입니다.")
        private String address;

        private LocalDateTime visitDateTime;

        private Double latitude;
        private Double longitude;
    }
}