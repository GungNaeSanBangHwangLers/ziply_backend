package ziply.auth.oauth.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) { }
