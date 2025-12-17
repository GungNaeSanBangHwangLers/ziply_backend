package ziply.review.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class HouseUpdateRequest {
    @NotBlank(message = "주소는 필수입니다.")
    private String address;

    @NotNull(message = "방문 예정 시간은 필수입니다.")
    private LocalDateTime visitDateTime;
}