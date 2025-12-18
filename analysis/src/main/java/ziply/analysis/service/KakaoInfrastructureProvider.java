package ziply.analysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Map;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoInfrastructureProvider {

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    private final WebClient webClient = WebClient.create("https://dapi.kakao.com");

    public int getCategoryCount(double lat, double lon, String categoryCode, int radius) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v2/local/search/category.json")
                            .queryParam("category_group_code", categoryCode).queryParam("x", lon)  // 경도
                            .queryParam("y", lat)  // 위도
                            .queryParam("radius", radius).build()).header("Authorization", "KakaoAK " + kakaoApiKey)
                    .retrieve().bodyToMono(Map.class).block(); // 인프라 조회는 동기적으로 결과를 받아야 하므로 .block() 사용

            if (response != null && response.containsKey("meta")) {
                Map<String, Object> meta = (Map<String, Object>) response.get("meta");
                return (int) meta.get("total_count");
            }
        } catch (Exception e) {
            log.error("카카오 인프라 API 호출 실패: category={}, lat={}, lon={}", categoryCode, lat, lon, e);
        }
        return 0;
    }
}