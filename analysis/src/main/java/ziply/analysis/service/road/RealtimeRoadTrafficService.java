package ziply.analysis.service.road;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ziply.analysis.dto.road.RealtimeRoadTrafficDto;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RealtimeRoadTrafficService {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public RealtimeRoadTrafficService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${seoul.traffic.api.key}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl("http://openapi.seoul.go.kr:8088").build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public List<RealtimeRoadTrafficDto> fetchRealtimeTraffic() {
        // ⭐️ 중요: 본인 키가 안되면 "sample"로 바꿔서 시도해보세요.
        String targetKey = "sample";
        String uri = String.format("/%s/json/RealtimeRoadTraffic/1/5/", targetKey);

        System.out.println("🌐 [DEBUG] 호출 URL: http://openapi.seoul.go.kr:8088" + uri);

        try {
            String responseStr = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // ⭐️ 서버가 보내는 실제 내용을 로그에 출력
            System.out.println("🌐 [DEBUG] API 원본 응답: " + responseStr);

            if (responseStr == null || responseStr.contains("<RESULT>") || responseStr.contains("INFO-100")) {
                System.err.println("❌ API 서버가 에러를 반환했습니다.");
                return List.of();
            }

            Map<String, Object> resMap = objectMapper.readValue(responseStr, Map.class);
            if (resMap.containsKey("RealtimeRoadTraffic")) {
                Map<String, Object> root = (Map<String, Object>) resMap.get("RealtimeRoadTraffic");
                List<Map<String, Object>> rows = (List<Map<String, Object>>) root.get("row");
                return rows.stream()
                        .map(row -> objectMapper.convertValue(row, RealtimeRoadTrafficDto.class))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("❌ API 통신 중 예외 발생: " + e.getMessage());
        }
        return List.of();
    }
}