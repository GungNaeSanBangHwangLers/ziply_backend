package ziply.auth.oauth;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ziply.auth.oauth.dto.response.TokenResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class OAuthController {
    private final GoogleOAuthService googleOAuthService;

    @GetMapping("/google")
    public void getGoogleLoginUrl(HttpServletResponse response) throws IOException {
        String url = googleOAuthService.getLoginUrl();
        log.info("Redirecting to Google OAuth: {}", url);
        response.sendRedirect(url);
    }

    @GetMapping("/google/callback")
    public ResponseEntity<TokenResponse> googleCallback(
            @RequestParam("code") String code
    ) {
        TokenResponse tokens = googleOAuthService.login(code);
        return ResponseEntity.ok(tokens);
    }
}
