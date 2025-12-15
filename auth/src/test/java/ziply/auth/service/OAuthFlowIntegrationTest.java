package ziply.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;
import ziply.auth.dto.request.GoogleLoginRequest;
import ziply.auth.dto.response.TokenResponse;
import ziply.auth.dto.response.UserSyncResponse;
import ziply.auth.jwt.GoogleTokenVerifier;
import ziply.auth.jwt.JwtTokenProvider;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class OAuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GoogleTokenVerifier googleTokenVerifier;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("전체 OAuth 로그인 플로우: Google 토큰 검증 -> User 서비스 호출 -> JWT 발급")
    void oauthLoginFlowIntegration() throws Exception {
        // given
        String idToken = "valid.google.id.token";
        String email = "integration@test.com";
        String name = "통합테스트";

        // Google 토큰 검증 Mock
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail(email);
        payload.set("name", name);
        when(googleTokenVerifier.verify(idToken)).thenReturn(payload);

        // User 서비스 응답 Mock
        UserSyncResponse userSyncResponse = new UserSyncResponse(1L, email, name);
        when(restTemplate.postForObject(
                anyString(),
                any(),
                eq(UserSyncResponse.class)
        )).thenReturn(userSyncResponse);

        GoogleLoginRequest request = new GoogleLoginRequest(idToken);

        // when & then
        mockMvc.perform(
                        post("/api/v1/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        // 응답에서 토큰 추출 및 검증
        String responseContent = mockMvc.perform(
                        post("/api/v1/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andReturn()
                .getResponse()
                .getContentAsString();

        TokenResponse tokenResponse = objectMapper.readValue(responseContent, TokenResponse.class);

        // JWT 토큰 검증
        assertThat(jwtTokenProvider.validateToken(tokenResponse.accessToken())).isTrue();
        assertThat(jwtTokenProvider.validateToken(tokenResponse.refreshToken())).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(tokenResponse.accessToken())).isEqualTo("1");
        assertThat(jwtTokenProvider.getUserIdFromToken(tokenResponse.refreshToken())).isEqualTo("1");
    }

    @Test
    @DisplayName("User 서비스 호출 실패 시 적절한 에러 응답을 반환한다")
    void oauthLoginFlowWhenUserServiceFails() throws Exception {
        // given
        String idToken = "valid.google.id.token";
        String email = "fail@test.com";
        String name = "실패테스트";

        // Google 토큰 검증 Mock
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail(email);
        payload.set("name", name);
        when(googleTokenVerifier.verify(idToken)).thenReturn(payload);

        // User 서비스 호출 실패 Mock
        when(restTemplate.postForObject(
                anyString(),
                any(),
                eq(UserSyncResponse.class)
        )).thenThrow(new RestClientException("User service unavailable"));

        GoogleLoginRequest request = new GoogleLoginRequest(idToken);

        // when & then
        mockMvc.perform(
                        post("/api/v1/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"))
                .andExpect(jsonPath("$.message").value("로그인에 실패했습니다."));
    }
}

