package ziply.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "유저 생성 요청 DTO (AUTH → USER)")
public record UserCreateRequest(
        @Schema(description = "이메일")
        @Email
        @NotBlank
        String email,

        @Schema(description = "이름")
        @NotBlank
        String name
) {
}
