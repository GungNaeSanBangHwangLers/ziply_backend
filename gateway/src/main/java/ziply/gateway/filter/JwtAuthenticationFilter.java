package ziply.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ziply.gateway.jwt.JwtTokenProvider;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;

    // JWT 검증이 필요 없는 경로들
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/v1/auth/google",           // Google 로그인
            "/api/v1/auth/refresh",          // 토큰 갱신
            "/swagger-ui",                   // Swagger UI
            "/v3/api-docs",                  // OpenAPI docs
            "/actuator"                      // Spring Actuator
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 제외 경로인지 확인
        if (isExcludedPath(path)) {
            log.debug("[JWT Filter] Excluded path: {}", path);
            return chain.filter(exchange);
        }

        // Authorization 헤더에서 JWT 추출
        String authHeader = request.getHeaders().getFirst("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[JWT Filter] Missing or invalid Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7); // "Bearer " 제거

        // JWT 검증
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("[JWT Filter] Invalid JWT token for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // JWT에서 userId 추출
        Long userId = jwtTokenProvider.getUserId(token);
        
        if (userId == null) {
            log.error("[JWT Filter] Failed to extract userId from token");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        log.info("[JWT Filter] Authenticated user: {} for path: {}", userId, path);

        // 요청 헤더에 userId 추가 (다운스트림 서비스에서 사용)
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", userId.toString())
                .build();

        // 수정된 요청으로 체인 계속 진행
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -100; // 로깅 필터보다 먼저 실행
    }
}
