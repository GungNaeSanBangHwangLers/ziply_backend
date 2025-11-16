package ziply.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "유저 수정 요청 DTO")
public record UserUpdateRequest(
        @Schema(description = "이름")
        @NotBlank
        String name
) {
}
