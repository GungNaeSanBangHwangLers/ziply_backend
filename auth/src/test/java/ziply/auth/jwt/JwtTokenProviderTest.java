package ziply.auth.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private final String TEST_SECRET = "abcdefghijklmnopqrstuvwxyz123456";
    private final long ACCESS_VALIDITY = 1000 * 60 * 10;
    private final long REFRESH_VALIDITY = 1000 * 60 * 60;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();

        ReflectionTestUtils.setField(jwtTokenProvider, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenValidityMs", ACCESS_VALIDITY);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenValidityMs", REFRESH_VALIDITY);

        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("Access Token 생성 시 userId(subject)가 정상적으로 포함된다")
    void generateAccessTokenSuccess() {
        String userId = "123";

        String token = jwtTokenProvider.generateAccessToken(userId);

        String subject = jwtTokenProvider.getUserIdFromToken(token);

        assertThat(subject).isEqualTo(userId);
    }

    @Test
    @DisplayName("Refresh Token 생성 시 userId(subject)가 정상적으로 포함된다")
    void generateRefreshTokenSuccess() {
        String userId = "123";

        String token = jwtTokenProvider.generateRefreshToken(userId);

        String subject = jwtTokenProvider.getUserIdFromToken(token);

        assertThat(subject).isEqualTo(userId);
    }

    @Test
    @DisplayName("유효한 JWT 토큰은 validateToken()이 true를 반환한다")
    void validateTokenValid() {
        String token = jwtTokenProvider.generateAccessToken("123");

        boolean isValid = jwtTokenProvider.validateToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("변조된(비정상) 토큰은 validateToken()이 false를 반환한다")
    void validateTokenInvalid() {
        String invalidToken = "xxxx.yyyy.zzzz";

        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 validateToken()이 false를 반환한다")
    void validateTokenExpired() {
        String expiredToken = Jwts.builder()
                .setSubject("123")
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60))
                .setExpiration(new Date(System.currentTimeMillis() - 1000)) // 이미 만료
                .signWith(
                        io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                                TEST_SECRET.getBytes(StandardCharsets.UTF_8)
                        ),
                        SignatureAlgorithm.HS256
                )
                .compact();

        boolean isValid = jwtTokenProvider.validateToken(expiredToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("생성한 토큰에서 userId를 정상적으로 추출할 수 있다")
    void getUserIdFromTokenSuccess() {
        String token = jwtTokenProvider.generateAccessToken("42");

        String userId = jwtTokenProvider.getUserIdFromToken(token);

        assertThat(userId).isEqualTo("42");
    }
}
