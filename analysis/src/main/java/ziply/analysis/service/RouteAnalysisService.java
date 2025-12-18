package ziply.analysis.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ziply.analysis.domain.HouseAnalysis;
import ziply.analysis.dto.response.BasePointAnalysisDto;
import ziply.analysis.dto.response.HouseAnalysisResultDto;
import ziply.analysis.dto.response.SearchCardAnalysisResponse;
import ziply.analysis.event.HouseCreatedEvent;
import ziply.analysis.event.HouseUpdatedEvent;
import ziply.analysis.repository.HouseInfrastructureRepository;
import ziply.analysis.repository.HouseRouteAnalysisRepository;
import ziply.analysis.service.KakaoRouteProvider.RouteResult;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteAnalysisService {

    private static final double AVG_WALKING_SPEED_KM_H = 4.5;
    private static final int MINUTES_IN_HOUR = 60;

    private final HouseRouteAnalysisRepository routeAnalysisRepository;
    private final HouseInfrastructureRepository houseInfrastructureRepository;
    private final KakaoRouteProvider kakaoRouteProvider;
    private final KakaoInfrastructureService kakaoInfrastructureService;
    private final NoiseScoringService noiseScoringService;
    private final WebClient.Builder webClientBuilder;

    @Transactional
    public void processHouseCreation(HouseCreatedEvent event) {
        List<AnalysisPoint> points = event.getBasePoints().stream()
                .map(p -> new AnalysisPoint(p.getId(), p.getName(), p.getLatitude(), p.getLongitude())).toList();

        processAnalysis(event.getSearchCardId(), event.getHouseId(), event.getLatitude(), event.getLongitude(), points);
    }

    @Transactional
    public void processHouseUpdate(HouseUpdatedEvent event) {
        routeAnalysisRepository.deleteByHouseId(event.getHouseId());
        houseInfrastructureRepository.deleteByHouseId(event.getHouseId());

        if (event.getBasePoints() == null) {
            return;
        }

        List<AnalysisPoint> points = event.getBasePoints().stream()
                .map(p -> new AnalysisPoint(p.getId(), p.getName(), p.getLatitude(), p.getLongitude())).toList();

        processAnalysis(event.getSearchCardId(), event.getHouseId(), event.getLatitude(), event.getLongitude(), points);
    }

    private void processAnalysis(UUID searchCardId, Long houseId, double lat, double lon, List<AnalysisPoint> points) {
        kakaoInfrastructureService.analyzeInfrastructure(houseId, lat, lon);

        int dayScore = noiseScoringService.calculateDayNoiseScore(houseId, lat, lon);
        int nightScore = noiseScoringService.calculateNightNoiseScore(houseId, lat, lon);
        ;

        for (AnalysisPoint point : points) {
            try {
                RouteResult route = kakaoRouteProvider.getWalkingRoute(lat, lon, point.lat(), point.lon());
                saveCommon(searchCardId, houseId, point, route, dayScore, nightScore);
            } catch (Exception e) {
                log.error("Analysis failed: HouseId={}, PointId={}", houseId, point.id(), e);
            }
        }
    }

    private void saveCommon(UUID searchCardId, Long houseId, AnalysisPoint point, RouteResult result, Integer dayScore,
                            Integer nightScore) {
        double distanceKm = result.distanceMeters() / 1000.0;
        int timeMin = (distanceKm > 0) ? (int) Math.round((distanceKm / AVG_WALKING_SPEED_KM_H) * MINUTES_IN_HOUR) : 0;

        HouseAnalysis analysis = HouseAnalysis.builder().searchCardId(searchCardId).houseId(houseId)
                .basePointId(point.id()).basePointName(point.name()).walkingTimeMin(timeMin)
                .walkingDistanceKm(distanceKm).dayScore(dayScore).nightScore(nightScore).build();

        routeAnalysisRepository.save(analysis);
    }

    @Transactional(readOnly = true)
    public SearchCardAnalysisResponse getAnalysisByCard(UUID searchCardId, Long userId) {
            Boolean isOwner = org.springframework.web.reactive.function.client.WebClient.create("http://localhost:8082")
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/review/card/{searchCardId}/owner-check")
                            .queryParam("userId", userId)
                            .build(searchCardId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            Mono.error(new AccessDeniedException("리뷰 서비스 권한 확인 중 오류가 발생했습니다.")))
                    .bodyToMono(Boolean.class)
                    .block(); // 권한 확인이 끝날 때까지 동기 대기

        if (isOwner == null || !isOwner) {
            log.warn("[Security Audit] Unauthorized card access: user={}, card={}", userId, searchCardId);
            throw new AccessDeniedException("해당 카드에 대한 접근 권한이 없습니다.");
        }

        List<HouseAnalysis> allData = routeAnalysisRepository.findBySearchCardId(searchCardId);

        if (allData.isEmpty()) {
            return new SearchCardAnalysisResponse(searchCardId, Collections.emptyList());
        }

        Map<Long, List<HouseAnalysis>> grouped = allData.stream()
                .collect(Collectors.groupingBy(HouseAnalysis::getBasePointId));

        List<BasePointAnalysisDto> basePointDtos = grouped.entrySet().stream().map(entry -> {
            List<HouseAnalysis> results = entry.getValue();
            results.sort(Comparator.comparing(HouseAnalysis::getWalkingTimeMin));
            return new BasePointAnalysisDto(entry.getKey(), results.get(0).getBasePointName(),
                    results.stream().map(HouseAnalysisResultDto::from).toList());
        }).toList();

        return new SearchCardAnalysisResponse(searchCardId, basePointDtos);
    }

    private record AnalysisPoint(Long id, String name, double lat, double lon) {
    }
}