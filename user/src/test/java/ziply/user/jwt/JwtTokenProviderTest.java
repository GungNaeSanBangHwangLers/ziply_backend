package ziply.user.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // HS256은 최소 32바이트 이상 필요
    private static final String SECRET = "this_is_a_very_secure_secret_key_123456";

    @BeforeEach
    void setUp() throws Exception {
        jwtTokenProvider = new JwtTokenProvider();

        // @Value로 주입되는 secretKey를 리플렉션으로 세팅
        Field secretKeyField = JwtTokenProvider.class.getDeclaredField("secretKey");
        secretKeyField.setAccessible(true);
        secretKeyField.set(jwtTokenProvider, SECRET);
    }

    private String createTokenWithSubject(String subject) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(subject)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void 유효한_토큰이면_validateToken_true_반환() {
        String token = createTokenWithSubject("123");

        boolean result = jwtTokenProvider.validateToken(token);

        assertThat(result).isTrue();
    }

    @Test
    void 변조된_토큰이면_validateToken_false_반환() {
        String token = createTokenWithSubject("123");
        String invalidToken = token + "x"; // 끝에 쓰레기 붙여서 변조

        boolean result = jwtTokenProvider.validateToken(invalidToken);

        assertThat(result).isFalse();
    }

    @Test
    void 유효한_토큰에서_getUserId_정상_파싱() {
        String token = createTokenWithSubject("123");

        Long userId = jwtTokenProvider.getUserId(token);

        assertThat(userId).isEqualTo(123L);
    }
}
