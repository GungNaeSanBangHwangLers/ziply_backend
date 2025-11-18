package ziply.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ziply.auth.jwt.GoogleTokenVerifier;
import ziply.auth.jwt.JwtTokenProvider;
import ziply.auth.dto.request.UserSyncRequest;
import ziply.auth.dto.response.TokenResponse;
import ziply.auth.dto.response.UserSyncResponse;
import ziply.auth.exception.AuthException;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate = new RestTemplate();

    public TokenResponse loginWithIdToken(String idToken) {
        log.info("loginWithIdToken() called");

        try {

            // 1. idToken null 체크
            if (idToken == null || idToken.isBlank()) {
                log.warn("[AUTH] Failed: idToken is null or empty");
                throw new AuthException("AUTH_FAILED");
            }

            // 2. ID Token 검증
            GoogleIdToken.Payload payload = googleTokenVerifier.verify(idToken);
            if (payload == null) {
                log.warn("[AUTH] Failed: Google ID Token verification returned null");
                throw new AuthException("AUTH_FAILED");
            }

            // 3. 정보 추출
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            if (name == null || name.isBlank()) {
                name = email;
            }

            log.info("[AUTH] Google verified user email={}", email);

            // 4. USER 서비스 연동
            Long userId = syncUserWithUserService(email, name);
            if (userId == null) {
                log.error("[AUTH] Failed: user sync returned null userId");
                throw new AuthException("AUTH_FAILED");
            }

            // 5. JWT 발급
            String accessToken = jwtTokenProvider.generateAccessToken(String.valueOf(userId));
            String refreshToken = jwtTokenProvider.generateRefreshToken(String.valueOf(userId));

            return new TokenResponse(accessToken, refreshToken);

        } catch (AuthException e) {
            // 직접 던진 AuthException은 그대로 다시 던짐
            throw e;
        } catch (Exception e) {
            log.error("[AUTH] Unknown error during loginWithIdToken", e);
            throw new AuthException("AUTH_FAILED");
        }
    }

    private Long syncUserWithUserService(String email, String name) {
        UserSyncRequest request = new UserSyncRequest(email, name);

        try {
            UserSyncResponse response = restTemplate.postForObject(
                    "http://localhost:8080/api/v1/users",
                    request,
                    UserSyncResponse.class
            );

            if (response == null || response.id() == null) {
                log.error("[AUTH] USER-SERVICE returned null response or null id");
                throw new AuthException("AUTH_FAILED");
            }

            return response.id();

        } catch (Exception e) {
            log.error("[AUTH] Failed to call USER-SERVICE", e);
            throw new AuthException("AUTH_FAILED");
        }
    }
}
