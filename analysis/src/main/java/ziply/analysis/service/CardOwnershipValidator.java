package ziply.analysis.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CardOwnershipValidator {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.review.url:http://review-service:8080}")
    private String reviewServiceUrl;

    public void validate(UUID searchCardId, Long userId) {
        webClientBuilder.build().get()
                .uri(reviewServiceUrl + "/api/v1/review/card/{searchCardId}/owner-check?userId={userId}",
                        searchCardId, userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, cr -> Mono.error(new AccessDeniedException("권한 확인 실패")))
                .bodyToMono(Boolean.class)
                .map(isOwner -> {
                    if (!isOwner) throw new AccessDeniedException("접근 권한이 없습니다.");
                    return isOwner;
                }).block();
    }
}
