package ziply.review.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private static final String SECRET = "this_is_a_very_secure_secret_key_for_review_service_123456";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", SECRET);
    }

    private String createTokenWithSubject(String subject) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(subject)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    @DisplayName("유효한 JWT 토큰은 validateToken()이 true를 반환한다")
    void validateTokenValid() {
        // given
        String token = createTokenWithSubject("123");

        // when
        boolean result = jwtTokenProvider.validateToken(token);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("변조된 토큰은 validateToken()이 false를 반환한다")
    void validateTokenInvalid() {
        // given
        String token = createTokenWithSubject("123");
        String invalidToken = token + "x";

        // when
        boolean result = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("잘못된 형식의 토큰은 validateToken()이 false를 반환한다")
    void validateTokenMalformed() {
        // given
        String invalidToken = "xxxx.yyyy.zzzz";

        // when
        boolean result = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("유효한 토큰에서 userId를 정상적으로 추출할 수 있다")
    void getUserIdSuccess() {
        // given
        String token = createTokenWithSubject("123");

        // when
        Long userId = jwtTokenProvider.getUserId(token);

        // then
        assertThat(userId).isEqualTo(123L);
    }

    @Test
    @DisplayName("다른 userId로 생성한 토큰에서도 정상적으로 추출할 수 있다")
    void getUserIdDifferentValue() {
        // given
        String token = createTokenWithSubject("999");

        // when
        Long userId = jwtTokenProvider.getUserId(token);

        // then
        assertThat(userId).isEqualTo(999L);
    }
}


