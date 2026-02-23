package ziply.analysis.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ziply.analysis.domain.HouseAnalysis;
import ziply.analysis.dto.response.BasePointAnalysisDto;
import ziply.analysis.dto.response.HouseAnalysisDto;
import ziply.analysis.dto.response.LifeScoreAnalysisResponse;
import ziply.analysis.dto.response.SafetyAnalysisResponse;
import ziply.analysis.dto.response.TransitResult;
import ziply.analysis.event.HouseCreatedEvent;
import ziply.analysis.event.HouseUpdatedEvent;
import ziply.analysis.repository.HouseInfrastructureRepository;
import ziply.analysis.repository.HouseRouteAnalysisRepository;
import ziply.analysis.service.KakaoRouteProvider.RouteResult;
import ziply.analysis.service.safety.SafetyScoringService;
import ziply.analysis.service.safety.SafetyScoringService.SafetyAnalysisResult;

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
    private final SafetyScoringService safetyScoringService;
    private final WebClient.Builder webClientBuilder;
    private final OdsayTransitProvider transitProvider;

    @Value("${services.review.url:http://review-service:8080}")
    private String reviewServiceUrl;

    // --- 이벤트 처리 로직 (기존 유지) ---

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

        if (event.getBasePoints() == null) return;

        List<AnalysisPoint> points = event.getBasePoints().stream()
                .map(p -> new AnalysisPoint(p.getId(), p.getName(), p.getLatitude(), p.getLongitude())).toList();
        processAnalysis(event.getSearchCardId(), event.getHouseId(), event.getLatitude(), event.getLongitude(), points);
    }

    private void processAnalysis(UUID searchCardId, Long houseId, double lat, double lon, List<AnalysisPoint> points) {
        kakaoInfrastructureService.analyzeInfrastructure(houseId, lat, lon);
        int dayScore = noiseScoringService.calculateDayNoiseScore(houseId, lat, lon);
        int nightScore = noiseScoringService.calculateNightNoiseScore(houseId, lat, lon);

        SafetyAnalysisResult safetyResult = safetyScoringService.analyzeSafety(lat, lon);

        List<CompletableFuture<Void>> futures = points.stream().map(point -> CompletableFuture.runAsync(() -> {
            try {
                RouteResult walk = kakaoRouteProvider.getWalkingRoute(lat, lon, point.lat(), point.lon());
                TransitResult transit = transitProvider.getTransitRoute(lat, lon, point.lat(), point.lon());
                RouteResult car = kakaoRouteProvider.getCarRoute(lat, lon, point.lat(), point.lon());
                saveCommon(searchCardId, houseId, point, walk, transit, car, dayScore, nightScore, safetyResult);
            } catch (Exception e) {
                log.error("Analysis failed in Async: HouseId={}, PointId={}", houseId, point.id(), e);
            }
        })).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void saveCommon(UUID searchCardId, Long houseId, AnalysisPoint point, RouteResult walk,
                            TransitResult transit, RouteResult car, Integer dayScore, Integer nightScore,
                            SafetyAnalysisResult safetyResult) {

        int carTimeMin = (car.durationSeconds() > 0) ? (int) Math.round(car.durationSeconds() / (double) MINUTES_IN_HOUR) : 0;
        double distanceKm = walk.distanceMeters() / 1000.0;
        int walkTimeMin = (distanceKm > 0) ? (int) Math.round((distanceKm / AVG_WALKING_SPEED_KM_H) * MINUTES_IN_HOUR) : 0;

        HouseAnalysis analysis = HouseAnalysis.builder()
                .searchCardId(searchCardId).houseId(houseId).basePointId(point.id()).basePointName(point.name())
                .walkingTimeMin(walkTimeMin).walkingDistanceKm(distanceKm).transitTimeMin(transit.timeMin())
                .transitPaymentStr(transit.paymentStr()).transitDepth(transit.transitCount()).carTimeMin(carTimeMin)
                .dayScore(dayScore).nightScore(nightScore).safetyScore(safetyResult.getScore())
                .policeCount(safetyResult.getPoliceCount()).streetlightCount(safetyResult.getStreetlightCount())
                .cctvCount(safetyResult.getCctvCount()).build();

        routeAnalysisRepository.save(analysis);
    }

    // --- 데이터 조회 로직 (분리 완료) ---

    /**
     * [1] 소음 및 생활 점수 분석 결과 반환
     */
    @Transactional(readOnly = true)
    public List<LifeScoreAnalysisResponse> getLifeScoreAnalysis(UUID searchCardId, Long userId) {
        checkCardOwner(searchCardId, userId);
        List<HouseAnalysis> allData = routeAnalysisRepository.findBySearchCardId(searchCardId);
        if (allData.isEmpty()) return Collections.emptyList();

        Map<Long, String> labelMap = createHouseLabelMap(allData);

        return allData.stream()
                .collect(Collectors.groupingBy(HouseAnalysis::getHouseId))
                .entrySet().stream()
                .map(entry -> {
                    Long houseId = entry.getKey();
                    HouseAnalysis first = entry.getValue().get(0);

                    return houseInfrastructureRepository.findByHouseId(houseId)
                            .map(infra -> LifeScoreAnalysisResponse.builder()
                                    .houseId(houseId)
                                    .label(labelMap.get(houseId))
                                    .dayScore(first.getDayScore())
                                    .nightScore(first.getNightScore())
                                    .schoolCount(infra.getSchoolCount())
                                    .subwayCount(infra.getSubwayCount())
                                    .message(String.format("이 점수는 인근 도로 트래픽, 버스 운행량, 학교(%d곳) 및 지하철역(%d곳) 밀집도를 반영했어요.",
                                            infra.getSchoolCount(), infra.getSubwayCount()))
                                    .build())
                            .orElseGet(() -> LifeScoreAnalysisResponse.builder()
                                    .houseId(houseId).label(labelMap.get(houseId))
                                    .dayScore(first.getDayScore()).nightScore(first.getNightScore())
                                    .message("인프라 분석 정보를 생성 중입니다.").build());
                })
                .sorted(Comparator.comparing(LifeScoreAnalysisResponse::getHouseId))
                .toList();
    }

    /**
     * [2] 안전 분석 결과 반환
     */
    @Transactional(readOnly = true)
    public List<SafetyAnalysisResponse> getSafetyAnalysis(UUID searchCardId, Long userId) {
        checkCardOwner(searchCardId, userId);
        List<HouseAnalysis> allData = routeAnalysisRepository.findBySearchCardId(searchCardId);
        if (allData.isEmpty()) return Collections.emptyList();

        Map<Long, String> labelMap = createHouseLabelMap(allData);

        return allData.stream()
                .collect(Collectors.groupingBy(HouseAnalysis::getHouseId))
                .entrySet().stream()
                .map(entry -> {
                    Long houseId = entry.getKey();
                    HouseAnalysis first = entry.getValue().get(0);

                    return SafetyAnalysisResponse.builder()
                            .houseId(houseId)
                            .label(labelMap.get(houseId))
                            .safetyScore(first.getSafetyScore())
                            .policeCount(first.getPoliceCount())
                            .streetlightCount(first.getStreetlightCount())
                            .cctvCount(first.getCctvCount())
                            .message(String.format("가로등 개수 %d개, 경찰서 %d개, CCTV %d개가 존재해요.",
                                    first.getStreetlightCount(), first.getPoliceCount(), first.getCctvCount()))
                            .build();
                })
                .sorted(Comparator.comparing(SafetyAnalysisResponse::getHouseId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BasePointAnalysisDto> getSearchCardDistanceAnalysis(UUID searchCardId, Long userId) {
        checkCardOwner(searchCardId, userId);
        List<HouseAnalysis> allData = routeAnalysisRepository.findBySearchCardId(searchCardId);
        if (allData.isEmpty()) return Collections.emptyList();

        Map<Long, String> houseLabelMap = createHouseLabelMap(allData);
        Map<Long, List<HouseAnalysis>> grouped = allData.stream().collect(Collectors.groupingBy(HouseAnalysis::getBasePointId));

        return grouped.entrySet().stream().map(entry -> {
            List<HouseAnalysis> results = entry.getValue();
            results.sort(Comparator.comparing(HouseAnalysis::getWalkingTimeMin));

            List<String> feeDetails = new java.util.ArrayList<>();
            for (HouseAnalysis entity : results) {
                String label = houseLabelMap.get(entity.getHouseId());
                String priceStr = (entity.getTransitPaymentStr() != null) ? entity.getTransitPaymentStr().replaceAll("[^0-9]", "") : "";
                int rawPrice = priceStr.isEmpty() ? 0 : Integer.parseInt(priceStr);
                feeDetails.add(String.format("%s는 %,d원", label, rawPrice * 2 * 30));
            }

            String transportMessage = String.format("한 달(30일) 기준 왕복 교통비는 %s이에요.", String.join(", ", feeDetails));
            List<HouseAnalysisDto> houseDtos = results.stream().map(e -> HouseAnalysisDto.from(e, houseLabelMap.get(e.getHouseId()))).toList();

            return new BasePointAnalysisDto(houseDtos, transportMessage);
        }).toList();
    }

    // --- 헬퍼 메서드 ---

    private Map<Long, String> createHouseLabelMap(List<HouseAnalysis> allData) {
        List<Long> distinctHouseIds = allData.stream().map(HouseAnalysis::getHouseId).distinct().sorted().toList();
        Map<Long, String> labelMap = new java.util.HashMap<>();
        for (int i = 0; i < distinctHouseIds.size(); i++) {
            labelMap.put(distinctHouseIds.get(i), String.valueOf((char) ('A' + i)));
        }
        return labelMap;
    }

    private void checkCardOwner(UUID searchCardId, Long userId) {
        log.info("[CHECK] Calling review-service to check owner. URL: {}", reviewServiceUrl);
        Boolean isOwner = webClientBuilder.build().get()
                .uri(reviewServiceUrl + "/api/v1/review/card/{searchCardId}/owner-check?userId={userId}", searchCardId, userId)
                .retrieve().onStatus(HttpStatusCode::isError, cr -> Mono.error(new AccessDeniedException("권한 확인 실패")))
                .bodyToMono(Boolean.class).block();

        if (isOwner == null || !isOwner) throw new AccessDeniedException("접근 권한이 없습니다.");
    }

    private record AnalysisPoint(Long id, String name, double lat, double lon) {}
}