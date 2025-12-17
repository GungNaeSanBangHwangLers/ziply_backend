package ziply.analysis.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.analysis.domain.HouseRouteAnalysis;
import ziply.analysis.dto.response.BasePointAnalysisDto;
import ziply.analysis.dto.response.HouseAnalysisResultDto;
import ziply.analysis.dto.response.SearchCardAnalysisResponse;
import ziply.analysis.event.HouseCreatedEvent;
import ziply.analysis.event.HouseUpdatedEvent;
import ziply.analysis.repository.HouseRouteAnalysisRepository;
import ziply.analysis.service.KakaoRouteProvider.RouteResult;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteAnalysisService {

    private static final double AVG_WALKING_SPEED_KM_H = 4.5;

    private final HouseRouteAnalysisRepository routeAnalysisRepository;
    private final KakaoRouteProvider kakaoRouteProvider;

    @Transactional
    public void processHouseCreation(HouseCreatedEvent event) {
        final double houseLat = event.getLatitude();
        final double houseLon = event.getLongitude();
        final UUID searchCardId = event.getSearchCardId();

        event.getBasePoints().forEach(basePoint -> {
            try {
                RouteResult result = kakaoRouteProvider.getWalkingRoute(houseLat, houseLon, basePoint.getLatitude(),
                        basePoint.getLongitude());

                // CreatedEvent 타입임을 명시
                saveRouteAnalysisResult(searchCardId, event.getHouseId(), basePoint, result);

            } catch (Exception e) {
                log.error("경로 분석 중 오류 발생: HouseId={}, BasePointId={}", event.getHouseId(), basePoint.getId(), e);
                throw new RuntimeException("경로 분석 중 오류가 발생하여 트랜잭션을 롤백합니다.", e);
            }
        });
    }

    @Transactional
    public void processHouseUpdate(HouseUpdatedEvent event) {
        log.info("[Analyst] House 수정 데이터 재계산 시작. HouseId: {}", event.getHouseId());

        routeAnalysisRepository.deleteByHouseId(event.getHouseId());

        double houseLat = event.getLatitude();
        double houseLon = event.getLongitude();

        if (event.getBasePoints() != null) {
            event.getBasePoints().forEach(basePoint -> {
                try {
                    RouteResult routeResult = kakaoRouteProvider.getWalkingRoute(houseLat, houseLon,
                            basePoint.getLatitude(), basePoint.getLongitude());

                    // UpdatedEvent 타입임을 명시
                    saveRouteAnalysisResult(event.getSearchCardId(), event.getHouseId(), basePoint, routeResult);

                } catch (Exception e) {
                    log.error("수정 경로 분석 중 오류 발생: HouseId={}, BasePointId={}", event.getHouseId(), basePoint.getId(), e);
                }
            });
        }
        log.info("[Analyst] House 수정 데이터 재계산 완료. HouseId: {}", event.getHouseId());
    }

    // --- 오버로딩 1: CreatedEvent용 (Full Name 사용으로 충돌 방지) ---
    private void saveRouteAnalysisResult(UUID searchCardId, Long houseId,
                                         ziply.analysis.event.HouseCreatedEvent.BasePointDetail basePoint,
                                         RouteResult result) {
        saveCommon(searchCardId, houseId, basePoint.getId(), basePoint.getName(), result);
    }

    // --- 오버로딩 2: UpdatedEvent용 (Full Name 사용으로 충돌 방지) ---
    private void saveRouteAnalysisResult(UUID searchCardId, Long houseId,
                                         ziply.analysis.event.HouseUpdatedEvent.BasePointDetail basePoint,
                                         RouteResult result) {
        saveCommon(searchCardId, houseId, basePoint.getId(), basePoint.getName(), result);
    }

    private void saveCommon(UUID searchCardId, Long houseId, Long basePointId, String basePointName,
                            RouteResult result) {
        int distanceMeters = result.distanceMeters();
        Double walkingDistanceKm = distanceMeters / 1000.0;
        Integer walkingTimeMin = 0;

        if (distanceMeters > 0) {
            double walkingTimeHour = walkingDistanceKm / AVG_WALKING_SPEED_KM_H;
            walkingTimeMin = (int) Math.round(walkingTimeHour * 60);
        }

        HouseRouteAnalysis analysis = HouseRouteAnalysis.builder().searchCardId(searchCardId).houseId(houseId)
                .basePointId(basePointId).basePointName(basePointName).walkingTimeMin(walkingTimeMin)
                .walkingDistanceKm(walkingDistanceKm).build();

        routeAnalysisRepository.save(analysis);
        log.info("-> 직주거리 저장 완료: BasePointId={}, Time={}분", basePointId, walkingTimeMin);
    }

    @Transactional(readOnly = true)
    public SearchCardAnalysisResponse getAnalysisByCard(UUID searchCardId, Long userId) {

        Long count = routeAnalysisRepository.countBySearchCardIdAndUserId(searchCardId, userId);

        if (count == null || count <= 0) {
            throw new AccessDeniedException("해당 카드에 대한 접근 권한이 없거나 존재하지 않는 카드입니다.");
        }

        List<HouseRouteAnalysis> allData = routeAnalysisRepository.findBySearchCardId(searchCardId);

        if (allData.isEmpty()) {
            return new SearchCardAnalysisResponse(searchCardId, Collections.emptyList());
        }

        Map<Long, List<HouseRouteAnalysis>> grouped = allData.stream()
                .collect(Collectors.groupingBy(HouseRouteAnalysis::getBasePointId));

        List<BasePointAnalysisDto> basePointDtos = grouped.entrySet().stream().map(entry -> {
            List<HouseRouteAnalysis> results = entry.getValue();
            results.sort(Comparator.comparing(HouseRouteAnalysis::getWalkingTimeMin));
            return new BasePointAnalysisDto(entry.getKey(), results.get(0).getBasePointName(),
                    results.stream().map(HouseAnalysisResultDto::from).toList());
        }).toList();

        return new SearchCardAnalysisResponse(searchCardId, basePointDtos);
    }
}