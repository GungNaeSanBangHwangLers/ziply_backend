package ziply.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class GatewayConfig implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // 요청 로깅
        log.info("[GATEWAY] {} {} from {}", 
                request.getMethod(), 
                request.getURI(), 
                request.getRemoteAddress());

        // 응답 후 로깅
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            log.info("[GATEWAY] Response: {} for {} {}", 
                    response.getStatusCode(), 
                    request.getMethod(), 
                    request.getURI());
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}



