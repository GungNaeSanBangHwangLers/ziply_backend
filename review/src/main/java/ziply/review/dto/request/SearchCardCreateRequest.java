package ziply.review.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SearchCardCreateRequest {
    @Valid
    private String basePointAddress;

    @Valid
    private PastResidenceRequest pastResidence;

    @Valid
    @Size(max = 7)
    private List<HouseCreateRequest> houses;

    @Getter @Setter @NoArgsConstructor
    public static class PastResidenceRequest {
        @NotBlank
        private String address;

        private Double latitude;
        private Double longitude;

        @Size(max = 3, message = "만족했던 점은 최대 3개까지 선택 가능합니다.")
        private List<String> advantages;

        @Size(max = 3, message = "개선되길 바라는 점은 최대 3개까지 선택 가능합니다.")
        private List<String> disadvantages;
    }

    @Getter @Setter @NoArgsConstructor
    public static class HouseCreateRequest {
        private String address;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
        private LocalDateTime visitDateTime;
    }
}