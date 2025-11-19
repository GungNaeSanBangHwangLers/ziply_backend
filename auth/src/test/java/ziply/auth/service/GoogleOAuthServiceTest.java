package ziply.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import ziply.auth.dto.request.UserSyncRequest;
import ziply.auth.dto.response.TokenResponse;
import ziply.auth.dto.response.UserSyncResponse;
import ziply.auth.exception.AuthException;
import ziply.auth.jwt.GoogleTokenVerifier;
import ziply.auth.jwt.JwtTokenProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthServiceTest {

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RestTemplate restTemplate;

    private GoogleOAuthService googleOAuthService;

    @BeforeEach
    void setUp() {
        googleOAuthService = new GoogleOAuthService(googleTokenVerifier, jwtTokenProvider);
        ReflectionTestUtils.setField(googleOAuthService, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("정상적인 idToken이면 USER-SERVICE 연동 후 JWT(access/refresh) 발급")
    void loginWithIdTokenSuccess() {
        String idToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMzQ1NiJ9."
                + "eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20ifQ."
                + "dummy-signature";

        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("test@example.com");
        payload.set("name", "테스트 유저");

        when(googleTokenVerifier.verify(idToken)).thenReturn(payload);

        UserSyncResponse userSyncResponse = new UserSyncResponse(1L, "test@example.com", "테스트 유저");

        when(restTemplate.postForObject(
                eq("http://localhost:8080/api/v1/users"),
                any(UserSyncRequest.class),
                eq(UserSyncResponse.class)
        )).thenReturn(userSyncResponse);

        String expectedAccessToken = "eyJhbGciOiJIUzI1NiJ9."
                + "eyJ1c2VySWQiOiIxIiwidHlwIjoiYWNjZXNzIn0."
                + "access-signature";
        String expectedRefreshToken = "eyJhbGciOiJIUzI1NiJ9."
                + "eyJ1c2VySWQiOiIxIiwidHlwIjoicmVmcmVzaCJ9."
                + "refresh-signature";

        when(jwtTokenProvider.generateAccessToken("1")).thenReturn(expectedAccessToken);
        when(jwtTokenProvider.generateRefreshToken("1")).thenReturn(expectedRefreshToken);

        TokenResponse result = googleOAuthService.loginWithIdToken(idToken);

        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo(expectedAccessToken);
        assertThat(result.refreshToken()).isEqualTo(expectedRefreshToken);
    }

    @Test
    @DisplayName("idToken 이 null 이면 AuthException(AUTH_FAILED) 발생")
    void loginWithIdTokenNullIdToken() {
        assertThatThrownBy(() -> googleOAuthService.loginWithIdToken(null))
                .isInstanceOf(AuthException.class)
                .hasMessage("AUTH_FAILED");
    }

    @Test
    @DisplayName("idToken 이 공백이면 AuthException(AUTH_FAILED) 발생")
    void loginWithIdTokenBlankIdToken() {
        assertThatThrownBy(() -> googleOAuthService.loginWithIdToken("   "))
                .isInstanceOf(AuthException.class)
                .hasMessage("AUTH_FAILED");
    }

    @Test
    @DisplayName("GoogleTokenVerifier 가 null payload 를 반환하면 AuthException(AUTH_FAILED) 발생")
    void loginWithIdTokenGoogleVerifyReturnsNull() {
        // given
        String idToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6Ijc4OTAxMjMifQ."
                + "eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJlbWFpbCI6InVzZXJAZ21haWwuY29tIn0."
                + "dummy-signature";

        when(googleTokenVerifier.verify(idToken)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> googleOAuthService.loginWithIdToken(idToken))
                .isInstanceOf(AuthException.class)
                .hasMessage("AUTH_FAILED");
    }

    @Test
    @DisplayName("USER-SERVICE 가 null response 를 반환하면 AuthException(AUTH_FAILED) 발생")
    void loginWithIdTokenUserServiceReturnsNull() {
        // given
        String idToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMzQ1NiJ9."
                + "eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20ifQ."
                + "dummy-signature";

        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("test@example.com");
        payload.set("name", "테스트 유저");

        when(googleTokenVerifier.verify(idToken)).thenReturn(payload);

        when(restTemplate.postForObject(
                eq("http://localhost:8080/api/v1/users"),
                any(UserSyncRequest.class),
                eq(UserSyncResponse.class)
        )).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> googleOAuthService.loginWithIdToken(idToken))
                .isInstanceOf(AuthException.class)
                .hasMessage("AUTH_FAILED");
    }
}