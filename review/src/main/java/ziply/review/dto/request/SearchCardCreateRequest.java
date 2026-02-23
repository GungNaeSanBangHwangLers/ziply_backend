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

    private String basePointAddress;

    @Valid
    @Size(max = 7)
    private List<HouseCreateRequest> houses;

    @Getter @Setter @NoArgsConstructor
    public static class HouseCreateRequest {
        @NotBlank
        private String address;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
        private LocalDateTime visitDateTime;
    }
}