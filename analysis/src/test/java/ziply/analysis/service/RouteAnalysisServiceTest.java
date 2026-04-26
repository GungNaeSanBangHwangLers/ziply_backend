package ziply.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import ziply.analysis.domain.HouseAnalysis;
import ziply.analysis.dto.response.BasePointAnalysisDto;
import ziply.analysis.dto.response.LifeScoreAnalysisResponse;
import ziply.analysis.dto.response.SafetyAnalysisResponse;
import ziply.analysis.event.HouseUpdatedEvent;
import ziply.analysis.repository.HouseInfrastructureRepository;
import ziply.analysis.repository.HouseRouteAnalysisRepository;
import ziply.analysis.service.safety.SafetyScoringService;
import ziply.analysis.util.HouseLabelMapper;

@ExtendWith(MockitoExtension.class)
class RouteAnalysisServiceTest {

    @Mock
    private HouseRouteAnalysisRepository routeAnalysisRepository;
    @Mock
    private HouseInfrastructureRepository houseInfrastructureRepository;
    @Mock
    private KakaoRouteProvider kakaoRouteProvider;
    @Mock
    private KakaoInfrastructureService kakaoInfrastructureService;
    @Mock
    private NoiseScoringService noiseScoringService;
    @Mock
    private SafetyScoringService safetyScoringService;
    @Mock
    private OdsayTransitProvider transitProvider;
    @Mock
    private CardOwnershipValidator cardOwnershipValidator;

    private RouteAnalysisService routeAnalysisService;

    @BeforeEach
    void setUp() {
        routeAnalysisService = new RouteAnalysisService(
                routeAnalysisRepository,
                houseInfrastructureRepository,
                kakaoRouteProvider,
                kakaoInfrastructureService,
                noiseScoringService,
                safetyScoringService,
                transitProvider,
                cardOwnershipValidator,
                new HouseLabelMapper()
        );
    }

    @Test
    void getSearchCardDistanceAnalysisReturnsEmptyWhenNoData() {
        UUID searchCardId = UUID.randomUUID();
        when(routeAnalysisRepository.findBySearchCardId(searchCardId)).thenReturn(List.of());

        List<BasePointAnalysisDto> result = routeAnalysisService.getSearchCardDistanceAnalysis(searchCardId, 1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getSearchCardDistanceAnalysisParsesTransitPaymentString() {
        UUID searchCardId = UUID.randomUUID();

        HouseAnalysis houseA = HouseAnalysis.builder()
                .searchCardId(searchCardId)
                .houseId(10L)
                .basePointId(1L)
                .basePointName("회사")
                .walkingTimeMin(15)
                .bikeTimeMin(5)
                .transitTimeMin(20)
                .transitPaymentStr("1,200원")
                .carTimeMin(10)
                .build();

        HouseAnalysis houseB = HouseAnalysis.builder()
                .searchCardId(searchCardId)
                .houseId(20L)
                .basePointId(1L)
                .basePointName("회사")
                .walkingTimeMin(30)
                .bikeTimeMin(9)
                .transitTimeMin(40)
                .transitPaymentStr(null)
                .carTimeMin(18)
                .build();

        when(routeAnalysisRepository.findBySearchCardId(searchCardId)).thenReturn(List.of(houseA, houseB));

        List<BasePointAnalysisDto> result = routeAnalysisService.getSearchCardDistanceAnalysis(searchCardId, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).transportMessage()).contains("A는 72,000원");
        assertThat(result.get(0).transportMessage()).contains("B는 정보 없음");
    }

    @Test
    void getSearchCardDistanceAnalysisThrowsWhenUserIsNotOwner() {
        UUID searchCardId = UUID.randomUUID();
        doThrow(new AccessDeniedException("접근 권한이 없습니다."))
                .when(cardOwnershipValidator).validate(searchCardId, 1L);

        assertThatThrownBy(() -> routeAnalysisService.getSearchCardDistanceAnalysis(searchCardId, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void processHouseUpdateReturnsEarlyWhenBasePointsAreNull() {
        HouseUpdatedEvent event = HouseUpdatedEvent.builder()
                .houseId(100L)
                .searchCardId(UUID.randomUUID())
                .latitude(37.5)
                .longitude(127.0)
                .basePoints(null)
                .build();

        routeAnalysisService.processHouseUpdate(event);

        verify(routeAnalysisRepository).deleteByHouseId(100L);
        verify(houseInfrastructureRepository).deleteByHouseId(100L);
        verify(kakaoInfrastructureService, never()).analyzeInfrastructure(anyLong(), anyDouble(), anyDouble());
        verify(routeAnalysisRepository, never()).save(any(HouseAnalysis.class));
    }

    @Test
    void getLifeScoreAnalysisReturnsEmptyWhenNoData() {
        UUID searchCardId = UUID.randomUUID();
        when(routeAnalysisRepository.findBySearchCardId(searchCardId)).thenReturn(List.of());

        List<LifeScoreAnalysisResponse> result = routeAnalysisService.getLifeScoreAnalysis(searchCardId, 1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getLifeScoreAnalysisUsesFallbackMessageWhenInfraMissing() {
        UUID searchCardId = UUID.randomUUID();

        HouseAnalysis house = HouseAnalysis.builder()
                .searchCardId(searchCardId)
                .houseId(10L)
                .basePointId(1L)
                .dayScore(65)
                .nightScore(70)
                .build();
        when(routeAnalysisRepository.findBySearchCardId(searchCardId)).thenReturn(List.of(house));
        when(houseInfrastructureRepository.findByHouseId(10L)).thenReturn(Optional.empty());

        List<LifeScoreAnalysisResponse> result = routeAnalysisService.getLifeScoreAnalysis(searchCardId, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessage()).contains("인프라 분석 정보를 생성 중입니다");
    }

    @Test
    void getSafetyAnalysisReturnsSortedByHouseId() {
        UUID searchCardId = UUID.randomUUID();

        HouseAnalysis highId = HouseAnalysis.builder()
                .searchCardId(searchCardId)
                .houseId(20L)
                .basePointId(1L)
                .safetyScore(80)
                .policeCount(1)
                .streetlightCount(10)
                .cctvCount(3)
                .build();
        HouseAnalysis lowId = HouseAnalysis.builder()
                .searchCardId(searchCardId)
                .houseId(10L)
                .basePointId(1L)
                .safetyScore(70)
                .policeCount(0)
                .streetlightCount(8)
                .cctvCount(2)
                .build();
        when(routeAnalysisRepository.findBySearchCardId(searchCardId)).thenReturn(List.of(highId, lowId));

        List<SafetyAnalysisResponse> result = routeAnalysisService.getSafetyAnalysis(searchCardId, 1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getHouseId()).isEqualTo(10L);
        assertThat(result.get(1).getHouseId()).isEqualTo(20L);
    }
}
