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

    private static final String SECRET = "this_is_a_very_secure_secret_key_123456";

    @BeforeEach
    void setUp() throws Exception {
        jwtTokenProvider = new JwtTokenProvider();

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
    void validateTokenReturnsTrueForValidToken() {
        String token = createTokenWithSubject("123");

        boolean result = jwtTokenProvider.validateToken(token);

        assertThat(result).isTrue();
    }

    @Test
    void validateTokenReturnsFalseForTamperedToken() {
        String token = createTokenWithSubject("123");
        String invalidToken = token + "x";

        boolean result = jwtTokenProvider.validateToken(invalidToken);

        assertThat(result).isFalse();
    }

    @Test
    void getUserIdParsesValidTokenCorrectly() {
        String token = createTokenWithSubject("123");

        Long userId = jwtTokenProvider.getUserId(token);

        assertThat(userId).isEqualTo(123L);
    }
}