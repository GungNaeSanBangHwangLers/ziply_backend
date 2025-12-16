package ziply.analysis.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import lombok.Getter;
import java.util.List;
import ziply.analysis.dto.response.KakaoRouteResponse.Route.Summary;

@Getter
public class KakaoRouteResponse {
    private List<Route> routes;

    @Getter
    public static class Route {
        private Summary summary;

        @Getter
        public static class Summary {
            @JsonProperty("duration")
            private int durationSeconds;
            @JsonProperty("distance")
            private int distanceMeters;
        }

        public Summary getSummary() {
            return summary;
        }
    }


    public int getDurationSeconds() {
        return Optional.ofNullable(routes).filter(list -> !list.isEmpty()).map(list -> list.get(0))
                .map(Route::getSummary).map(Summary::getDurationSeconds).orElse(0);
    }

    public int getDistanceMeters() {
        return Optional.ofNullable(routes).filter(list -> !list.isEmpty()).map(list -> list.get(0))
                .map(Route::getSummary).map(Summary::getDistanceMeters).orElse(0);
    }
}