package ziply.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import ziply.user.domain.user.User;
import ziply.user.domain.user.UserStatus;

@Schema(description = "유저 응답 DTO")
public record UserResponse(
        @Schema(description = "유저 ID")
        Long id,
        @Schema(description = "이메일")
        String email,
        @Schema(description = "이름")
        String name,
        @Schema(description = "상태")
        UserStatus status,
        @Schema(description = "생성 시각")
        LocalDateTime createdAt,
        @Schema(description = "수정 시각")
        LocalDateTime updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}