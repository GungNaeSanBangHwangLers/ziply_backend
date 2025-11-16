package ziply.auth.oauth.dto.request;

public record UserSyncRequest(
        String email,
        String name
) {
}

