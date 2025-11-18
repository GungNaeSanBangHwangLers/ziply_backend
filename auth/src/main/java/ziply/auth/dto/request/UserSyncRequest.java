package ziply.auth.dto.request;

public record UserSyncRequest(
        String email,
        String name
) {
}

