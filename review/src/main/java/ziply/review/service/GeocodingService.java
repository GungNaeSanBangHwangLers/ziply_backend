package ziply.review.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import ziply.review.dto.request.HouseCreateRequest;
import ziply.review.dto.response.KakaoGeocodeResponse;

@Slf4j
@Service
public class GeocodingService {

    private final WebClient webClient;
    private final String kakaoApiKey;
    private final String kakaoGeocodingUrl;

    public GeocodingService(WebClient.Builder webClientBuilder,
                            @Value("${kakao.api.key}") String kakaoApiKey,
                            @Value("${kakao.api.geocoding-url}") String kakaoGeocodingUrl) {
        this.webClient = webClientBuilder.baseUrl(kakaoGeocodingUrl).build();
        this.kakaoApiKey = kakaoApiKey;
        this.kakaoGeocodingUrl = kakaoGeocodingUrl;
    }

    public void geocodeAddress(HouseCreateRequest request) {
        String address = request.getAddress();

        log.info("Geocoding: Kakao API로 '{}' 주소의 좌표를 조회합니다.", address);

        String uri = UriComponentsBuilder.fromUriString(kakaoGeocodingUrl)
                .queryParam("query", address)
                .build()
                .toUriString();

        try {
            KakaoGeocodeResponse response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "KakaoAK " + kakaoApiKey)
                    .retrieve()
                    .bodyToMono(KakaoGeocodeResponse.class)
                    .block();

            if (response != null && !response.getDocuments().isEmpty()) {
                KakaoGeocodeResponse.Document doc = response.getDocuments().get(0);

                request.setLongitude(Double.parseDouble(doc.getX()));
                request.setLatitude(Double.parseDouble(doc.getY()));

                log.info("Geocoding 성공: 위도 {}, 경도 {}", request.getLatitude(), request.getLongitude());
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