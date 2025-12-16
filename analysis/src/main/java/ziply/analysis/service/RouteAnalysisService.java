package ziply.analysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ziply.analysis.domain.HouseRouteAnalysis;
import ziply.analysis.event.HouseCreatedEvent;
import ziply.analysis.event.HouseCreatedEvent.BasePointDetail;
import ziply.analysis.repository.HouseRouteAnalysisRepository;
import ziply.analysis.service.KakaoRouteProvider.RouteResult;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteAnalysisService {

    private static final double AVG_WALKING_SPEED_KM_H = 4.5;

    private final HouseRouteAnalysisRepository routeAnalysisRepository;
    private final KakaoRouteProvider kakaoRouteProvider;

    public void processHouseCreation(HouseCreatedEvent event) {

        final double houseLat = event.getLatitude();
        final double houseLon = event.getLongitude();

        event.getBasePoints().forEach(basePoint -> {
            try {
                RouteResult result = kakaoRouteProvider.getWalkingRoute(houseLat, houseLon, basePoint.getLatitude(),
                        basePoint.getLongitude());

                saveRouteAnalysisResult(event.getHouseId(), basePoint, result);

            } catch (Exception e) {
                log.error("경로 분석 중 오류 발생: HouseId={}, BasePointId={}", event.getHouseId(), basePoint.getId(), e);
                throw new RuntimeException("경로 분석 중 오류가 발생하여 트랜잭션을 롤백합니다.", e);
            }
        });
    }

    private void saveRouteAnalysisResult(Long houseId, BasePointDetail basePoint, RouteResult result) {

        int distanceMeters = result.distanceMeters();
        Double walkingDistanceKm = distanceMeters / 1000.0;

        Integer walkingTimeMin = 0;

        if (distanceMeters > 0) {
            double walkingTimeHour = walkingDistanceKm / AVG_WALKING_SPEED_KM_H;

            walkingTimeMin = (int) Math.round(walkingTimeHour * 60);
        }

        HouseRouteAnalysis analysis = HouseRouteAnalysis.builder().houseId(houseId).basePointId(basePoint.getId())
                .walkingTimeMin(walkingTimeMin).walkingDistanceKm(walkingDistanceKm).build();

        routeAnalysisRepository.save(analysis);

        log.info("-> 직주거리 저장 완료 (자체 계산): BasePointId={}, Time={}분, Distance={}km", basePoint.getId(), walkingTimeMin,
                String.format("%.2f", walkingDistanceKm));
    }
}