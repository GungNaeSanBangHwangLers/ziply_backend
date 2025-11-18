package ziply.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ziply.auth.dto.request.GoogleLoginRequest;
import ziply.auth.dto.response.TokenResponse;
import ziply.auth.service.GoogleOAuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class OAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoogleOAuthService googleOAuthService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/auth/google : ID 토큰으로 로그인 시 200과 JWT 토큰 응답")
    void googleLoginMobile_success() throws Exception {
        String idToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMzQ1NiJ9."
                + "eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20ifQ."
                + "dummy-signature";

        GoogleLoginRequest request = new GoogleLoginRequest(idToken);

        TokenResponse tokenResponse = new TokenResponse(
                "eyJhbGciOiJIUzI1NiJ9."
                        + "eyJ1c2VySWQiOiIxMjMiLCJ0eXAiOiJhY2Nlc3MifQ."
                        + "access-signature",
                "eyJhbGciOiJIUzI1NiJ9."
                        + "eyJ1c2VySWQiOiIxMjMiLCJ0eXAiOiJyZWZyZXNoIn0."
                        + "refresh-signature"
        );

        Mockito.when(googleOAuthService.loginWithIdToken(anyString()))
                .thenReturn(tokenResponse);

        mockMvc.perform(
                        post("/api/v1/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value(tokenResponse.accessToken()))
                .andExpect(jsonPath("$.refreshToken").value(tokenResponse.refreshToken()));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(googleOAuthService, Mockito.times(1))
                .loginWithIdToken(captor.capture());

        String passedIdToken = captor.getValue();
        assertThat(passedIdToken).isEqualTo(idToken);
    }
}
