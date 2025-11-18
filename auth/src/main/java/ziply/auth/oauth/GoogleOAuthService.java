package ziply.auth.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ziply.auth.jwt.JwtTokenProvider;
import ziply.auth.oauth.dto.request.UserSyncRequest;
import ziply.auth.oauth.dto.response.TokenResponse;
import ziply.auth.oauth.dto.response.UserSyncResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate = new RestTemplate();

     //안드로이드 전용 로그인 (idToken 기반)
    public TokenResponse loginWithIdToken(String idToken) {
        log.info("loginWithIdToken() called");

        // 1. ID Token 검증
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(idToken);

        String email = payload.getEmail();
        String name = (String) payload.get("name");

        if (name == null || name.isBlank()) {
            name = email; // name 없으면 email로 대체
        }

        // 2. USER 서비스와 연동
        Long userId = syncUserWithUserService(email, name);

        // 3. 우리 JWT 발급
        String accessToken = jwtTokenProvider.generateAccessToken(String.valueOf(userId));
        String refreshToken = jwtTokenProvider.generateRefreshToken(String.valueOf(userId));

        return new TokenResponse(accessToken, refreshToken);
    }

    private Long syncUserWithUserService(String email, String name) {
        UserSyncRequest request = new UserSyncRequest(email, name);

        UserSyncResponse response = restTemplate.postForObject(
                "http://localhost:8080/api/v1/users",
                request,
                UserSyncResponse.class
        );

        if (response == null || response.id() == null) {
            throw new IllegalStateException("USER 서비스에서 유저 정보를 가져오지 못했습니다.");
        }

        return response.id();
    }
}
