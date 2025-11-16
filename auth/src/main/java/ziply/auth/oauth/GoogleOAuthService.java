package ziply.auth.oauth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ziply.auth.jwt.JwtTokenProvider;
import ziply.auth.oauth.dto.request.UserSyncRequest;
import ziply.auth.oauth.dto.response.GoogleTokenResponse;
import ziply.auth.oauth.dto.response.GoogleUserInfoResponse;
import ziply.auth.oauth.dto.response.TokenResponse;
import ziply.auth.oauth.dto.response.UserSyncResponse;

@Service
@Slf4j
public class GoogleOAuthService {

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    @Value("${google.clientId}")
    private String clientId;

    @Value("${google.redirectUri}")
    private String redirectUri;

    @Value("${google.clientSecret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    private final JwtTokenProvider jwtTokenProvider;

    public GoogleOAuthService(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    String getLoginUrl() {
        String url = UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid profile email")
                .queryParam("access_type", "offline")
                .build()
                .toUriString();

        log.info("GOOGLE LOGIN URL = {}", url);  // ★ 추가

        return url;
    }


    public TokenResponse login(String code) {
        // 1. 구글 토큰 요청
        log.info("login() received code={}", code);
        GoogleTokenResponse googleToken = requestGoogleToken(code);

        // 2. 구글 유저 정보 요청
        GoogleUserInfoResponse userInfo = requestGoogleUserInfo(googleToken.accessToken());

        // 3. USER 서비스에 회원 생성/조회 요청
        Long userId = syncUserWithUserService(userInfo);

        // 4. JWT 생성
        String accessToken = jwtTokenProvider.generateAccessToken(String.valueOf(userId));
        String refreshToken = jwtTokenProvider.generateRefreshToken(String.valueOf(userId));

        return new TokenResponse(accessToken, refreshToken);
    }

    private GoogleTokenResponse requestGoogleToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");
        return restTemplate.postForObject(
                GOOGLE_TOKEN_URL,
                params,
                GoogleTokenResponse.class
        );
    }

    private GoogleUserInfoResponse requestGoogleUserInfo(String accessToken) {
        String uri = UriComponentsBuilder.fromHttpUrl(GOOGLE_USERINFO_URL)
                .queryParam("access_token", accessToken)
                .build()
                .toUriString();

        return restTemplate.getForObject(
                uri,
                GoogleUserInfoResponse.class
        );
    }

    private Long syncUserWithUserService(GoogleUserInfoResponse userInfo) {
        UserSyncRequest request = new UserSyncRequest(
                userInfo.email(),
                userInfo.name()
        );

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