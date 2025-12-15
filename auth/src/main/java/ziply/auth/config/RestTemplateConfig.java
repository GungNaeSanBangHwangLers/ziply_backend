package ziply.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .interceptors(new LoggingInterceptor())
                .errorHandler(new CustomResponseErrorHandler())
                .build();
    }

    private static class LoggingInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(
                HttpRequest request,
                byte[] body,
                ClientHttpRequestExecution execution) throws IOException {
            log.debug("[AUTH] RestTemplate Request: {} {}", request.getMethod(), request.getURI());
            ClientHttpResponse response = execution.execute(request, body);
            log.debug("[AUTH] RestTemplate Response: {}", response.getStatusCode());
            return response;
        }
    }

    private static class CustomResponseErrorHandler extends DefaultResponseErrorHandler {
        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            log.error("[AUTH] RestTemplate Error: Status {} - {}", 
                    response.getStatusCode(), response.getStatusText());
            super.handleError(response);
        }
    }
}
