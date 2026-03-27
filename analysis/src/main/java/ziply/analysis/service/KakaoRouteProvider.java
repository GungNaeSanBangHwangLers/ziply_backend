package ziply.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ziply.analysis.dto.response.KakaoRouteResponse;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoRouteProvider {

    @Value("${kakao.api.key}")
    private String kakaoRestApiKey;

    private final WebClient webClient = WebClient.create("https://apis-navi.kakaomobility.com");
    private static final String DIRECTION_ENDPOINT = "/v1/directions";

    public record RouteResult(int durationSeconds, int distanceMeters) {
    }

    public RouteResult getWalkingRoute(double startLat, double startLon, double endLat, double endLon) {
        String origin = String.format("%f,%f", startLon, startLat);
        String destination = String.format("%f,%f", endLon, endLat);

        KakaoRouteResponse response;
        try {
            response = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path(DIRECTION_ENDPOINT).queryParam("origin", origin)
                            .queryParam("destination", destination).queryParam("priority", "RECOMMEND")
                            .queryParam("profile", "walking").build())
                    .header("Authorization", "KakaoAK " + kakaoRestApiKey).retrieve()
                    .bodyToMono(KakaoRouteResponse.class).block();

        } catch (Exception e) {
            log.error("카카오 경로 API 호출 실패: Origin={}, Dest={}", origin, destination, e);
            throw new RuntimeException("카카오 API 호출 실패", e);
        }
        if (response == null || response.getRoutes() == null || response.getRoutes().isEmpty()) {
            log.warn("카카오 API: 경로를 찾을 수 없음 (Empty Route). Origin={}, Dest={}", origin, destination);
            return new RouteResult(0, 0);
        }
        return new RouteResult(0, response.getDistanceMeters());
    }

    public RouteResult getCarRoute(double startLat, double startLon, double endLat, double endLon) {
        String origin = String.format("%f,%f", startLon, startLat);
        String destination = String.format("%f,%f", endLon, endLat);

        String departureTime = getNextWeekdayEightAM();

        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path(DIRECTION_ENDPOINT)
                            .queryParam("origin", origin)
                            .queryParam("destination", destination)
                            .queryParam("origin_time", departureTime) // 출근 시간 적용
                            .queryParam("priority", "RECOMMEND")
                            .build())
                    .header("Authorization", "KakaoAK " + kakaoRestApiKey)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || response.at("/routes/0").isMissingNode()) {
                log.warn("카카오 자동차 API: 경로를 찾을 수 없음. Origin={}, Dest={}", origin, destination);
                return new RouteResult(0, 0);
            }

            JsonNode summary = response.at("/routes/0/summary");
            int duration = summary.path("duration").asInt();
            int distance = summary.path("distance").asInt();

            return new RouteResult(duration, distance);

        } catch (Exception e) {
            log.error("카카오 자동차 경로 API 호출 실패 (출근시간 모드): {}", e.getMessage());
            return new RouteResult(0, 0);
        }
    }

    private String getNextWeekdayEightAM() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target = now.withHour(8).withMinute(0).withSecond(0).withNano(0);

        if (target.isBefore(now) || isWeekend(target)) {
            target = target.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        }

        return target.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }

    private boolean isWeekend(LocalDateTime date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}