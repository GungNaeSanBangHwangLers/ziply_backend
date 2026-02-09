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
import ziply.analysis.dto.response.SearchCardDistanceAnalysis;
import ziply.analysis.dto.response.SearchCardScoreAnalysis;
import ziply.analysis.dto.response.TransitResult;
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
    private final OdsayTransitProvider transitProvider;

    @Value("${services.review.url:http://localhost:8080}")
    private String reviewServiceUrl;

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

        List<CompletableFuture<Void>> futures = points.stream().map(point -> CompletableFuture.runAsync(() -> {
            try {
                RouteResult walk = kakaoRouteProvider.getWalkingRoute(lat, lon, point.lat(), point.lon());
                TransitResult transit = transitProvider.getTransitRoute(lat, lon, point.lat(), point.lon());
                RouteResult car = kakaoRouteProvider.getCarRoute(lat, lon, point.lat(), point.lon());

                saveCommon(searchCardId, houseId, point, walk, transit, car, dayScore, nightScore);
            } catch (Exception e) {
                log.error("Analysis failed in Async: HouseId={}, PointId={}", houseId, point.id(), e);
            }
        })).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void saveCommon(UUID searchCardId, Long houseId, AnalysisPoint point, RouteResult walk,
                            TransitResult transit, RouteResult car, Integer dayScore, Integer nightScore) {

        int carTimeMin =
                (car.durationSeconds() > 0) ? (int) Math.round(car.durationSeconds() / (double) MINUTES_IN_HOUR) : 0;

        double distanceKm = walk.distanceMeters() / 1000.0;

        int walkTimeMin =
                (distanceKm > 0) ? (int) Math.round((distanceKm / AVG_WALKING_SPEED_KM_H) * MINUTES_IN_HOUR) : 0;

        HouseAnalysis analysis = HouseAnalysis.builder().searchCardId(searchCardId).houseId(houseId)
                .basePointId(point.id()).basePointName(point.name()).walkingTimeMin(walkTimeMin)
                .walkingDistanceKm(distanceKm).transitTimeMin(transit.timeMin()).transitPaymentStr(transit.paymentStr())
                .transitDepth(transit.transitCount()).carTimeMin(carTimeMin).dayScore(dayScore).nightScore(nightScore)
                .build();

        routeAnalysisRepository.save(analysis);
    }

    @Transactional(readOnly = true)
    public SearchCardDistanceAnalysis getSearchCardDistanceAnalysis(UUID searchCardId, Long userId) {
        checkCardOwner(searchCardId, userId);

        List<HouseAnalysis> allData = routeAnalysisRepository.findBySearchCardId(searchCardId);
        if (allData.isEmpty()) {
            return new SearchCardDistanceAnalysis(Collections.emptyList());
        }

        List<Long> distinctHouseIds = allData.stream()
                .map(HouseAnalysis::getHouseId)
                .distinct()
                .sorted()
                .toList();

        Map<Long, String> houseLabelMap = new java.util.HashMap<>();
        for (int i = 0; i < distinctHouseIds.size(); i++) {
            houseLabelMap.put(distinctHouseIds.get(i), String.valueOf((char) ('A' + i)));
        }

        Map<Long, List<HouseAnalysis>> grouped = allData.stream()
                .collect(Collectors.groupingBy(HouseAnalysis::getBasePointId));

        List<BasePointAnalysisDto> basePointDtos = grouped.entrySet().stream().map(entry -> {
            List<HouseAnalysis> results = entry.getValue();
            results.sort(Comparator.comparing(HouseAnalysis::getWalkingTimeMin));

            List<String> feeDetails = new java.util.ArrayList<>();
            for (HouseAnalysis entity : results) {
                String label = houseLabelMap.get(entity.getHouseId());
                int rawPrice = Integer.parseInt(entity.getTransitPaymentStr().replaceAll("[^0-9]", ""));
                int monthlyPrice = rawPrice * 2 * 30;
                feeDetails.add(String.format("%s는 %,d원", label, monthlyPrice));
            }

            String transportMessage = String.format(
                    "한 달(30일) 기준 왕복 교통비는 %s이에요.\n" +
                            "서울지역 대중교통은 기후동행카드를 통해 62,000원(만19~39세 55,000원)으로 30일 무제한 이용가능하니 고려해보세요.",
                    String.join(", ", feeDetails)
            );

            List<HouseAnalysisDto> houseDtos = results.stream()
                    .map(entity -> HouseAnalysisDto.from(entity, houseLabelMap.get(entity.getHouseId())))
                    .toList();

            return new BasePointAnalysisDto(houseDtos, transportMessage);
        }).toList();

        return new SearchCardDistanceAnalysis(basePointDtos);
    }

    @Transactional(readOnly = true)
    public List<SearchCardScoreAnalysis> getSearchCardScoreAnalysis(UUID searchCardId, Long userId) {
        checkCardOwner(searchCardId, userId);

        List<HouseAnalysis> allData = routeAnalysisRepository.findBySearchCardId(searchCardId);

        return allData.stream().collect(Collectors.groupingBy(HouseAnalysis::getHouseId)).entrySet().stream()
                .map(entry -> {
                    Long houseId = entry.getKey();

                    String dayMessage = generateScoreDescription(houseId);

                    List<HouseAnalysis> houseDataList = entry.getValue();
                    HouseAnalysis first = houseDataList.get(0);

                    SearchCardScoreAnalysis dto = new SearchCardScoreAnalysis();
                    dto.setHouseId(houseId);
                    dto.setDayScore(first.getDayScore());
                    dto.setNightScore(first.getNightScore());

                    double avg = (first.getDayScore() + (double) first.getNightScore()) / 2.0;
                    dto.setAvgScore(avg);
                    dto.setMessage(dayMessage);

                    return dto;
                }).sorted(Comparator.comparing(SearchCardScoreAnalysis::getHouseId)).collect(Collectors.toList());
    }

    private void checkCardOwner(UUID searchCardId, Long userId) {
        log.info("[CHECK] Calling review-service to check owner. URL: {}", reviewServiceUrl);

        Boolean isOwner = webClientBuilder.build().get()
                .uri(reviewServiceUrl + "/api/v1/review/card/{searchCardId}/owner-check?userId={userId}", searchCardId,
                        userId).retrieve().onStatus(HttpStatusCode::isError,
                        clientResponse -> Mono.error(new AccessDeniedException("리뷰 서비스 권한 확인 중 오류가 발생했습니다.")))
                .bodyToMono(Boolean.class).block();

        if (isOwner == null || !isOwner) {
            log.warn("[Security Audit] Unauthorized card access: user={}, card={}", userId, searchCardId);
            throw new AccessDeniedException("해당 카드에 대한 접근 권한이 없습니다.");
        }
    }

    private String generateScoreDescription(Long houseId) {
        return houseInfrastructureRepository.findByHouseId(houseId).map(infra -> {
            String template = "이 점수는 인근 도로 트래픽, 주간 버스 운행 수, 학교 (%d곳), 지상 지하철역(%d곳), 상권 밀도를 함께 반영해 계산됐어요.";
            return String.format(template, infra.getSchoolCount(), infra.getSubwayCount());
        }).orElse("주변 인프라 정보를 분석하고 있습니다. 잠시 후 다시 확인해주세요.");
    }

    private record AnalysisPoint(Long id, String name, double lat, double lon) {
    }
}