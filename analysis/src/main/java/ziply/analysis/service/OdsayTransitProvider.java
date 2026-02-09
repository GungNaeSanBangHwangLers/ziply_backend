package ziply.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ziply.analysis.dto.response.TransitResult;

@Slf4j
@Component
@RequiredArgsConstructor
public class OdsayTransitProvider {

    @Value("${odsay.api.key}")
    private String odsayApiKey;

    private final WebClient webClient = WebClient.create("https://api.odsay.com/v1/api");

    public TransitResult getTransitRoute(double startLat, double startLon, double endLat, double endLon) {
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/searchPubTransPathT")
                            .queryParam("apiKey", odsayApiKey)
                            .queryParam("SX", startLon)
                            .queryParam("SY", startLat)
                            .queryParam("EX", endLon)
                            .queryParam("EY", endLat)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || response.at("/result/path").isMissingNode()) {
                log.warn("ODsay API: 경로를 찾을 수 없음. Origin=({}, {}), Dest=({}, {})", startLat, startLon, endLat, endLon);
                return TransitResult.empty();
            }

            // 최적 경로(첫 번째 경로) 정보 추출
            JsonNode info = response.at("/result/path/0/info");

            int totalTime = info.path("totalTime").asInt();
            int payment = info.path("payment").asInt();

            int rawTransitCount = info.path("transitCount").asInt();
            int realTransferCount = Math.max(0, rawTransitCount - 1);

            return new TransitResult(totalTime, String.format("%,d원", payment), realTransferCount);

        } catch (Exception e) {
            log.error("ODsay API 호출 중 예외 발생", e);
            return TransitResult.empty();
        }
    }
}