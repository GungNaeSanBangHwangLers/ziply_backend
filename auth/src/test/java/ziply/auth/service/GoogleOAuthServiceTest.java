package ziply.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ziply.auth.config.ServiceProperties;
import ziply.auth.dto.request.UserSyncRequest;
import ziply.auth.dto.response.TokenResponse;
import ziply.auth.dto.response.UserSyncResponse;
import ziply.auth.exception.AuthException;
import ziply.auth.jwt.GoogleTokenVerifier;
import ziply.auth.jwt.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GoogleOAuthServiceTest {

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ServiceProperties serviceProperties;

    @InjectMocks
    private GoogleOAuthService googleOAuthService;

    private GoogleIdToken.Payload mockPayload;

    @BeforeEach
    void setUp() {
        mockPayload = new GoogleIdToken.Payload();
        mockPayload.setEmail("test@example.com");
        mockPayload.set("name", "홍길동");

        // ServiceProperties 구조 설정
        ServiceProperties.UserService userService = new ServiceProperties.UserService();
        userService.setBaseUrl("http://user-service:8080");
        
        ServiceProperties.UserService.Endpoint endpoint = new ServiceProperties.UserService.Endpoint();
        endpoint.setCreateUser("/api/v1/user/create");
        userService.setEndpoint(endpoint);

        when(serviceProperties.getUser()).thenReturn(userService);
    }

    @Test
    @DisplayName("로그인 성공 - 정상 케이스")
    void loginWithIdToken_Success() {
        // given
        String idToken = "valid-id-token";
        when(googleTokenVerifier.verify(idToken)).thenReturn(mockPayload);
        when(restTemplate.postForObject(anyString(), any(UserSyncRequest.class), eq(UserSyncResponse.class)))
                .thenReturn(new UserSyncResponse(1L, "test@example.com", "홍길동"));
        when(jwtTokenProvider.generateAccessToken("1")).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken("1")).thenReturn("refresh-token");

        // when
        TokenResponse result = googleOAuthService.loginWithIdToken(idToken);

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(googleTokenVerifier).verify(idToken);
        verify(restTemplate).postForObject(anyString(), any(UserSyncRequest.class), eq(UserSyncResponse.class));
        verify(jwtTokenProvider).generateAccessToken("1");
        verify(jwtTokenProvider).generateRefreshToken("1");
    }

    @Test
    @DisplayName("로그인 실패 - null idToken")
    void loginWithIdToken_NullIdToken_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> googleOAuthService.loginWithIdToken(null))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("AUTH_FAILED");

        verify(googleTokenVerifier, never()).verify(anyString());
    }

    @Test
    @DisplayName("로그인 실패 - 빈 idToken")
    void loginWithIdToken_EmptyIdToken_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> googleOAuthService.loginWithIdToken(""))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("AUTH_FAILED");

        verify(googleTokenVerifier, never()).verify(anyString());
    }

    @Test
    @DisplayName("로그인 실패 - Google Token 검증 실패")
    void loginWithIdToken_InvalidToken_ThrowsException() {
        // given
        String idToken = "invalid-id-token";
        when(googleTokenVerifier.verify(idToken)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> googleOAuthService.loginWithIdToken(idToken))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("AUTH_FAILED");

        verify(googleTokenVerifier).verify(idToken);
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    @DisplayName("로그인 실패 - User Service 연동 실패")
    void loginWithIdToken_UserServiceFailure_ThrowsException() {
        // given
        String idToken = "valid-id-token";
        when(googleTokenVerifier.verify(idToken)).thenReturn(mockPayload);
        when(restTemplate.postForObject(anyString(), any(UserSyncRequest.class), eq(UserSyncResponse.class)))
                .thenThrow(new RestClientException("Connection failed"));

        // when & then
        assertThatThrownBy(() -> googleOAuthService.loginWithIdToken(idToken))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("AUTH_FAILED");

        verify(googleTokenVerifier).verify(idToken);
        verify(restTemplate).postForObject(anyString(), any(UserSyncRequest.class), eq(UserSyncResponse.class));
        verify(jwtTokenProvider, never()).generateAccessToken(anyString());
    }

    @Test
    @DisplayName("로그인 실패 - User Service가 null userId 반환")
    void loginWithIdToken_NullUserId_ThrowsException() {
        // given
        String idToken = "valid-id-token";
        when(googleTokenVerifier.verify(idToken)).thenReturn(mockPayload);
        when(restTemplate.postForObject(anyString(), any(UserSyncRequest.class), eq(UserSyncResponse.class)))
                .thenReturn(new UserSyncResponse(null, "test@example.com", "홍길동"));

        // when & then
        assertThatThrownBy(() -> googleOAuthService.loginWithIdToken(idToken))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("AUTH_FAILED");
    }

    @Test
    @DisplayName("로그인 성공 - name이 없는 경우 email 사용")
    void loginWithIdToken_NoName_UsesEmail() {
        // given
        String idToken = "valid-id-token";
        GoogleIdToken.Payload payloadWithoutName = new GoogleIdToken.Payload();
        payloadWithoutName.setEmail("test@example.com");

        when(googleTokenVerifier.verify(idToken)).thenReturn(payloadWithoutName);
        when(restTemplate.postForObject(anyString(), any(UserSyncRequest.class), eq(UserSyncResponse.class)))
                .thenReturn(new UserSyncResponse(1L, "test@example.com", "test@example.com"));
        when(jwtTokenProvider.generateAccessToken("1")).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken("1")).thenReturn("refresh-token");

        // when
        TokenResponse result = googleOAuthService.loginWithIdToken(idToken);

        // then
        assertThat(result).isNotNull();
        verify(restTemplate).postForObject(anyString(), any(UserSyncRequest.class), eq(UserSyncResponse.class));
    }

    @Test
    @DisplayName("로그인 성공 - name이 빈 문자열인 경우 email 사용")
    void loginWithIdToken_BlankName_UsesEmail() {
        // given
        String idToken = "valid-id-token";
        GoogleIdToken.Payload payloadWithBlankName = new GoogleIdToken.Payload();
        payloadWithBlankName.setEmail("test@example.com");
        payloadWithBlankName.set("name", "");

        when(googleTokenVerifier.verify(idToken)).thenReturn(payloadWithBlankName);
        when(restTemplate.postForObject(anyString(), any(UserSyncRequest.class), eq(UserSyncResponse.class)))
                .thenReturn(new UserSyncResponse(1L, "test@example.com", "test@example.com"));
        when(jwtTokenProvider.generateAccessToken("1")).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken("1")).thenReturn("refresh-token");

        // when
        TokenResponse result = googleOAuthService.loginWithIdToken(idToken);

        // then
        assertThat(result).isNotNull();
        verify(restTemplate).postForObject(anyString(), any(UserSyncRequest.class), eq(UserSyncResponse.class));
    }

    @Test
    @DisplayName("로그인 실패 - User Service가 null response 반환")
    void loginWithIdToken_NullResponse_ThrowsException() {
        // given
        String idToken = "valid-id-token";
        when(googleTokenVerifier.verify(idToken)).thenReturn(mockPayload);
        when(restTemplate.postForObject(anyString(), any(UserSyncRequest.class), eq(UserSyncResponse.class)))
                .thenReturn(null);

        // when & then
        assertThatThrownBy(() -> googleOAuthService.loginWithIdToken(idToken))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("AUTH_FAILED");
    }
}
