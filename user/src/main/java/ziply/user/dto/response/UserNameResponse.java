package ziply.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import ziply.user.domain.user.User;

@Schema(description = "유저 응답 DTO")
public record UserNameResponse(
        @Schema(description = "이름")
        String name
) {
    public static UserNameResponse from(User user) {
        return new UserNameResponse(
                user.getName()
        );
    }
}