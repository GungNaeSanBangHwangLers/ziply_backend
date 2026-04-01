package ziply.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ziply.analysis.domain.HouseAnalysis;
import ziply.analysis.dto.response.BasePointAnalysisDto;
import ziply.analysis.event.HouseUpdatedEvent;
import ziply.analysis.repository.HouseInfrastructureRepository;
import ziply.analysis.repository.HouseRouteAnalysisRepository;
import ziply.analysis.service.safety.SafetyScoringService;

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
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient.Builder webClientBuilder;
    @Mock
    private OdsayTransitProvider transitProvider;

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
                webClientBuilder,
                transitProvider
        );
        ReflectionTestUtils.setField(routeAnalysisService, "reviewServiceUrl", "http://review-service:8080");
    }

    @Test
    void getSearchCardDistanceAnalysisReturnsEmptyWhenNoData() {
        UUID searchCardId = UUID.randomUUID();
        mockOwnerCheck(true);
        when(routeAnalysisRepository.findBySearchCardId(searchCardId)).thenReturn(List.of());

        List<BasePointAnalysisDto> result = routeAnalysisService.getSearchCardDistanceAnalysis(searchCardId, 1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getSearchCardDistanceAnalysisParsesTransitPaymentString() {
        UUID searchCardId = UUID.randomUUID();
        mockOwnerCheck(true);

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
        mockOwnerCheck(false);

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

    private void mockOwnerCheck(boolean isOwner) {
        when(webClientBuilder.build()
                .get()
                .uri(anyString(), any(), any())
                .retrieve()
                .onStatus(any(), any())
                .bodyToMono(eq(Boolean.class)))
                .thenReturn(Mono.just(isOwner));
    }
}
