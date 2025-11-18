package ziply.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ziply.auth.dto.request.GoogleLoginRequest;
import ziply.auth.dto.response.TokenResponse;
import ziply.auth.service.GoogleOAuthService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class OAuthController {

    private final GoogleOAuthService googleOAuthService;

    // [모바일] 구글 ID 토큰 로그인
    @Operation(summary = "모바일 구글 로그인", description = "안드로이드에서 Google Sign-In SDK로 로그인 후 획득한 ID 토큰을 전달하면서버에서 검증 후 JWT를 발급합니다.")
    @ApiResponse(responseCode = "200", description = "JWT 토큰 발급 완료", content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @PostMapping("/google")
    public ResponseEntity<TokenResponse> googleLoginMobile(
            @RequestBody(
                    description = "안드로이드에서 받은 구글 ID 토큰",
                    required = true,
                    content = @Content(schema = @Schema(implementation = GoogleLoginRequest.class))
            )
            GoogleLoginRequest request
    ) {
        TokenResponse tokens = googleOAuthService.loginWithIdToken(request.idToken());
        return ResponseEntity.ok(tokens);
    }
}
