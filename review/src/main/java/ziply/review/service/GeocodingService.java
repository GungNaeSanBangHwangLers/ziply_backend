package ziply.review.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import ziply.review.dto.request.HouseCreateRequest;
import ziply.review.dto.response.GeocodingResult;
import ziply.review.dto.response.KakaoGeocodeResponse;

@Slf4j
@Service
public class GeocodingService {

    private final WebClient webClient;
    private final String kakaoApiKey;
    private final String kakaoGeocodingUrl;

    public GeocodingService(WebClient.Builder webClientBuilder, @Value("${kakao.api.key}") String kakaoApiKey,
                            @Value("${kakao.api.geocoding-url}") String kakaoGeocodingUrl) {

        int pathIndex = kakaoGeocodingUrl.indexOf("/v2");
        String baseUrl = (pathIndex > 0) ? kakaoGeocodingUrl.substring(0, pathIndex) : kakaoGeocodingUrl;

        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.kakaoApiKey = kakaoApiKey;
        this.kakaoGeocodingUrl = kakaoGeocodingUrl;
    }

    public void geocodeAddress(HouseCreateRequest request) {
        String address = request.getAddress();

        log.info("Geocoding: Kakao API로 '{}' 주소의 좌표를 조회합니다.", address);

        String uri = UriComponentsBuilder.fromPath("/v2/local/search/address.json").queryParam("query", address).build()
                .toUriString();

        try {
            KakaoGeocodeResponse response = webClient.get().uri(uri).header("Authorization", "KakaoAK " + kakaoApiKey)
                    .retrieve().onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("Kakao API 오류 응답: " + body)))
                    .bodyToMono(KakaoGeocodeResponse.class).block();

            if (response != null && response.getDocuments() != null && !response.getDocuments().isEmpty()) {
                KakaoGeocodeResponse.Document doc = response.getDocuments().get(0);

                Double longitude = Double.parseDouble(doc.getX());
                Double latitude = Double.parseDouble(doc.getY());

                request.setLongitude(longitude);
                request.setLatitude(latitude);

                log.info("Geocoding 성공: 주소 '{}', 위도 {}, 경도 {}", address, latitude, longitude);

            } else {
                log.warn("Geocoding 실패: 주소 '{}'에 대한 좌표를 찾을 수 없습니다.", address);
                throw new IllegalArgumentException("유효하지 않거나 찾을 수 없는 주소입니다: " + address);
            }

        } catch (Exception e) {
            log.error("Geocoding API 호출 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("좌표 변환 중 오류가 발생했습니다.", e);
        }
    }


    public GeocodingResult geocodeAddress(String address) {
        log.info("Geocoding: Kakao API로 '{}' 주소의 좌표를 조회합니다.", address);

        String uri = UriComponentsBuilder.fromPath("/v2/local/search/address.json").queryParam("query", address).build()
                .toUriString();

        try {
            KakaoGeocodeResponse response = webClient.get().uri(uri).header("Authorization", "KakaoAK " + kakaoApiKey)
                    .retrieve().onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("Kakao API 오류 응답: " + body)))
                    .bodyToMono(KakaoGeocodeResponse.class).block();

            if (response != null && response.getDocuments() != null && !response.getDocuments().isEmpty()) {
                KakaoGeocodeResponse.Document doc = response.getDocuments().get(0);

                Double longitude = Double.parseDouble(doc.getX());
                Double latitude = Double.parseDouble(doc.getY());

                log.info("Geocoding 성공: 주소 '{}', 위도 {}, 경도 {}", address, latitude, longitude);

                return new GeocodingResult(latitude, longitude);
            } else {
                log.warn("Geocoding 실패: 주소 '{}'에 대한 좌표를 찾을 수 없습니다.", address);
                throw new IllegalArgumentException("유효하지 않거나 찾을 수 없는 주소입니다: " + address);
            }

        } catch (Exception e) {
            log.error("Geocoding API 호출 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("좌표 변환 중 오류가 발생했습니다.", e);
        }
    }
}