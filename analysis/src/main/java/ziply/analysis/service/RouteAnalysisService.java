package ziply.analysis.service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.analysis.domain.HouseAnalysis;
import ziply.analysis.dto.response.*;
import ziply.analysis.event.HouseCreatedEvent;
import ziply.analysis.event.HouseUpdatedEvent;
import ziply.analysis.repository.HouseInfrastructureRepository;
import ziply.analysis.repository.HouseRouteAnalysisRepository;
import ziply.analysis.service.KakaoRouteProvider.RouteResult;
import ziply.analysis.service.safety.SafetyScoringService;
import ziply.analysis.service.safety.SafetyScoringService.SafetyAnalysisResult;
import ziply.analysis.util.HouseLabelMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteAnalysisService {

    private static final double AVG_WALKING_SPEED_KM_H = 4.5;
    private static final double AVG_BIKE_SPEED_KM_H = 15.0;
    private static final int MINUTES_IN_HOUR = 60;

    private static final String CLIMATE_CARD_INFO = "서울지역 대중교통은 기후동행카드를 통해 62,000원(만19~39세 55,000원)으로 30일 무제한 이용가능하니 고려해보세요.";
    private static final String BIKE_RENTAL_INFO = "서울시 따릉이는 1일권(1일 1시간 기준 1,000원)과 30일 정기권(30일간 매일 1시간 이용, 5,000원)이 있어요.";

    private final HouseRouteAnalysisRepository routeAnalysisRepository;
    private final HouseInfrastructureRepository houseInfrastructureRepository;
    private final KakaoRouteProvider kakaoRouteProvider;
    private final KakaoInfrastructureService kakaoInfrastructureService;
    private final NoiseScoringService noiseScoringService;
    private final SafetyScoringService safetyScoringService;
    private final OdsayTransitProvider transitProvider;
    private final CardOwnershipValidator cardOwnershipValidator;
    private final HouseLabelMapper houseLabelMapper;

    @Transactional
    public void processHouseCreation(HouseCreatedEvent event) {
        List<AnalysisPoint> points = event.getBasePoints().stream()
                .map(p -> new AnalysisPoint(p.getId(), p.getName(), p.getLatitude(), p.getLongitude())).toList();
        processAnalysis(event.getSearchCardId(), event.getHouseId(), event.getLatitude(), event.getLongitude(), event.getRegionName(), points);
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
        processAnalysis(event.getSearchCardId(), event.getHouseId(), event.getLatitude(), event.getLongitude(), event.getRegionName(), points);
    }

    private void processAnalysis(UUID searchCardId, Long houseId, double lat, double lon, String regionName, List<AnalysisPoint> points) {
        kakaoInfrastructureService.analyzeInfrastructure(houseId, lat, lon);
        int dayScore = noiseScoringService.calculateDayNoiseScore(houseId, lat, lon);
        int nightScore = noiseScoringService.calculateNightNoiseScore(houseId, lat, lon);
        SafetyAnalysisResult safetyResult = safetyScoringService.analyzeSafety(lat, lon);

        List<CompletableFuture<Void>> futures = points.stream().map(point -> CompletableFuture.runAsync(() -> {
            try {
                RouteResult walk = kakaoRouteProvider.getWalkingRoute(lat, lon, point.lat(), point.lon());
                TransitResult transit = transitProvider.getTransitRoute(lat, lon, point.lat(), point.lon());
                RouteResult car = kakaoRouteProvider.getCarRoute(lat, lon, point.lat(), point.lon());

                saveCommon(searchCardId, houseId, regionName, point, walk, transit, car, dayScore, nightScore, safetyResult);
            } catch (Exception e) {
                log.error("Analysis failed in Async: HouseId={}, PointId={}", houseId, point.id(), e);
            }
        })).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void saveCommon(UUID searchCardId, Long houseId, String regionName, AnalysisPoint point, RouteResult walk,
                            TransitResult transit, RouteResult car, Integer dayScore, Integer nightScore,
                            SafetyAnalysisResult safetyResult) {

        int carTimeMin =
                (car.durationSeconds() > 0) ? (int) Math.round(car.durationSeconds() / (double) MINUTES_IN_HOUR) : 0;
        double distanceKm = walk.distanceMeters() / 1000.0;

        int walkTimeMin =
                (distanceKm > 0) ? (int) Math.round((distanceKm / AVG_WALKING_SPEED_KM_H) * MINUTES_IN_HOUR) : 0;
        int bikeTimeMin = (distanceKm > 0) ? (int) Math.round((distanceKm / AVG_BIKE_SPEED_KM_H) * MINUTES_IN_HOUR) : 0;

        if (transit.timeMin() == 0 && "정보 없음".equals(transit.paymentStr()) && walkTimeMin > 0) {
            transit = new TransitResult(0, "도보가 더 빨라요", 0);
        }

        HouseAnalysis analysis = HouseAnalysis.builder()
                .searchCardId(searchCardId).houseId(houseId).basePointId(point.id()).basePointName(point.name())
                .walkingTimeMin(walkTimeMin).walkingDistanceKm(distanceKm)
                .bikeTimeMin(bikeTimeMin)
                .transitTimeMin(transit.timeMin()).transitPaymentStr(transit.paymentStr())
                .transitDepth(transit.transitCount())
                .carTimeMin(carTimeMin)
                .dayScore(dayScore).nightScore(nightScore).safetyScore(safetyResult.getScore())
                .policeCount(safetyResult.getPoliceCount()).streetlightCount(safetyResult.getStreetlightCount())
                .cctvCount(safetyResult.getCctvCount())
                .regionName(regionName)
                .build();

        routeAnalysisRepository.save(analysis);
    }

    @Transactional(readOnly = true)
    public List<BasePointAnalysisDto> getSearchCardDistanceAnalysis(UUID searchCardId, Long userId) {
        cardOwnershipValidator.validate(searchCardId, userId);
        List<HouseAnalysis> allData = routeAnalysisRepository.findBySearchCardId(searchCardId);
        if (allData.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> houseLabelMap = houseLabelMapper.build(allData);
        Map<Long, List<HouseAnalysis>> grouped = allData.stream()
                .collect(Collectors.groupingBy(HouseAnalysis::getBasePointId));

        return grouped.entrySet().stream()
                .map(entry -> createBasePointAnalysis(entry.getValue(), houseLabelMap))
                .toList();
    }

    private BasePointAnalysisDto createBasePointAnalysis(
            List<HouseAnalysis> results,
            Map<Long, String> houseLabelMap) {

        results.sort(Comparator.comparing(HouseAnalysis::getWalkingTimeMin, Comparator.nullsLast(Integer::compareTo)));

        Map<Boolean, List<HouseAnalysis>> partitioned = results.stream()
                .collect(Collectors.partitioningBy(
                    e -> "도보가 더 빨라요".equals(e.getTransitPaymentStr())
                ));

        List<String> walkingLabels = partitioned.get(true).stream()
                .map(e -> houseLabelMap.get(e.getHouseId()))
                .toList();

        List<String> feeDetails = partitioned.get(false).stream()
                .map(e -> formatFeeDetail(e, houseLabelMap.get(e.getHouseId())))
                .toList();

        String transportMessage = buildTransportMessage(feeDetails, walkingLabels);

        String bikeMessage = (feeDetails.isEmpty() && !walkingLabels.isEmpty())
            ? null
            : BIKE_RENTAL_INFO;

        List<HouseAnalysisDto> houseDtos = results.stream()
                .map(e -> HouseAnalysisDto.from(e, houseLabelMap.get(e.getHouseId())))
                .toList();

        return new BasePointAnalysisDto(houseDtos, bikeMessage, transportMessage);
    }

    private String formatFeeDetail(HouseAnalysis entity, String label) {
        int price = extractPrice(entity.getTransitPaymentStr());
        return price > 0
                ? String.format("%s는 %,d원", label, price * 2 * 30)
                : String.format("%s는 정보 없음", label);
    }

    private int extractPrice(String paymentStr) {
        if (paymentStr == null) {
            return 0;
        }
        String priceStr = paymentStr.replaceAll("[^0-9]", "");
        return priceStr.isEmpty() ? 0 : Integer.parseInt(priceStr);
    }

    private String buildTransportMessage(List<String> feeDetails, List<String> walkingLabels) {
        StringBuilder sb = new StringBuilder();

        if (!feeDetails.isEmpty()) {
            sb.append("한 달(30일) 기준 왕복 교통비는 ")
              .append(String.join(", ", feeDetails))
              .append("이에요. ");
        }

        if (!walkingLabels.isEmpty()) {
            sb.append(String.join(", ", walkingLabels))
              .append("는 도보로 이동 가능해요. ");
        }

        if (!feeDetails.isEmpty()) {
            sb.append(CLIMATE_CARD_INFO);
        }

        return sb.toString().trim();
    }

    @Transactional(readOnly = true)
    public List<LifeScoreAnalysisResponse> getLifeScoreAnalysis(UUID searchCardId, Long userId) {
        cardOwnershipValidator.validate(searchCardId, userId);
        List<HouseAnalysis> allData = routeAnalysisRepository.findBySearchCardId(searchCardId);
        if (allData.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> labelMap = houseLabelMapper.build(allData);

        return allData.stream()
                .collect(Collectors.groupingBy(HouseAnalysis::getHouseId))
                .entrySet().stream()
                .map(entry -> {
                    Long houseId = entry.getKey();
                    HouseAnalysis first = entry.getValue().get(0);

                    return houseInfrastructureRepository.findByHouseId(houseId)
                            .map(infra -> LifeScoreAnalysisResponse.builder()
                                    .houseId(houseId).label(labelMap.get(houseId))
                                    .dayScore(first.getDayScore()).nightScore(first.getNightScore())
                                    .schoolCount(infra.getSchoolCount()).subwayCount(infra.getSubwayCount())
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

    @Transactional(readOnly = true)
    public List<SafetyAnalysisResponse> getSafetyAnalysis(UUID searchCardId, Long userId) {
        cardOwnershipValidator.validate(searchCardId, userId);
        List<HouseAnalysis> allData = routeAnalysisRepository.findBySearchCardId(searchCardId);
        if (allData.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> labelMap = houseLabelMapper.build(allData);

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

    private record AnalysisPoint(Long id, String name, double lat, double lon) {
    }
}
