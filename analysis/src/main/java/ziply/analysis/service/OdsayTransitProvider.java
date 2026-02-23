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
                            .queryParam("SearchPathType", 0) // 모든 수단(지하철+버스) 검색
                            .queryParam("SearchRadius", 2000) // 정류장 검색 반경을 2km로 확대 (도보 82분 대응)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            // 1. 응답 데이터 검증 로직 강화
            if (response == null || response.has("error")) {
                String errorMsg = response != null ? response.path("error").path("message").asText() : "Response is null";
                log.warn("ODsay API 에러 발생: {}", errorMsg);
                return TransitResult.empty();
            }

            JsonNode pathArray = response.at("/result/path");
            if (pathArray.isMissingNode() || !pathArray.isArray() || pathArray.isEmpty()) {
                log.warn("ODsay API: 경로를 찾을 수 없음 (결과 없음). Origin=({}, {}), Dest=({}, {})", startLat, startLon, endLat, endLon);
                return TransitResult.empty();
            }

            // 2. 최적 경로 추출 (가장 짧은 시간 기준 정렬되어 있음)
            JsonNode bestPath = pathArray.get(0);
            JsonNode info = bestPath.path("info");

            int totalTime = info.path("totalTime").asInt();
            int payment = info.path("payment").asInt();

            // 3. 환승 횟수 계산 로직 보정
            // ODsay에서 transitCount는 교통수단 탑승 횟수이므로, 환승 횟수는 탑승 횟수 - 1입니다.
            int busCount = info.path("busTransitCount").asInt();
            int subwayCount = info.path("subwayTransitCount").asInt();
            int totalTransitCount = busCount + subwayCount;
            int realTransferCount = Math.max(0, totalTransitCount - 1);

            log.info("[TransitSuccess] 시간: {}분, 요금: {}원, 환승: {}회", totalTime, payment, realTransferCount);

            return new TransitResult(
                    totalTime,
                    payment > 0 ? String.format("%,d원", payment) : "0원",
                    realTransferCount
            );

        } catch (Exception e) {
            log.error("ODsay API 호출 중 치명적 예외 발생: {}", e.getMessage());
            return TransitResult.empty();
        }
    }
}