package ziply.auth.dto.response;

public record UserSyncResponse(
        Long id,
        String email,
        String name
) {
}
