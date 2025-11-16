package ziply.auth.oauth.dto.response;

public record UserSyncResponse(
        Long id,
        String email,
        String name
) {
}
