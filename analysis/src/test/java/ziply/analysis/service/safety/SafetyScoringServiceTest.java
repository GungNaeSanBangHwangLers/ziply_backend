package ziply.analysis.service.safety;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ziply.analysis.repository.CCtvRepository;
import ziply.analysis.repository.PoliceStationRepository;
import ziply.analysis.repository.StreetLightRepository;
import ziply.analysis.service.safety.SafetyScoringService.SafetyAnalysisResult;

@ExtendWith(MockitoExtension.class)
class SafetyScoringServiceTest {

    @Mock
    private CCtvRepository cctvRepository;

    @Mock
    private PoliceStationRepository policeRepository;

    @Mock
    private StreetLightRepository streetLightRepository;

    @InjectMocks
    private SafetyScoringService safetyScoringService;

    @Test
    @DisplayName("안전도 분석 - 모든 시설물이 많은 경우 높은 점수")
    void analyzeSafety_HighFacilities_HighScore() {
        // given
        double lat = 37.5;
        double lon = 127.0;

        // 경찰서 2개, CCTV 500대, 가로등 200개
        when(policeRepository.countNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(2);
        when(cctvRepository.sumQtyNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(500);
        when(streetLightRepository.countInBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(200);

        // when
        SafetyAnalysisResult result = safetyScoringService.analyzeSafety(lat, lon);

        // then
        assertThat(result.getScore()).isEqualTo(100); // 모든 시설물이 MAX 이상이므로 100점
        assertThat(result.getPoliceCount()).isEqualTo(2);
        assertThat(result.getCctvCount()).isEqualTo(500);
        assertThat(result.getStreetlightCount()).isEqualTo(200);
    }

    @Test
    @DisplayName("안전도 분석 - 시설물이 적은 경우 낮은 점수")
    void analyzeSafety_LowFacilities_LowScore() {
        // given
        double lat = 37.5;
        double lon = 127.0;

        // 경찰서 0개, CCTV 50대, 가로등 20개
        when(policeRepository.countNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(0);
        when(cctvRepository.sumQtyNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(50);
        when(streetLightRepository.countInBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(20);

        // when
        SafetyAnalysisResult result = safetyScoringService.analyzeSafety(lat, lon);

        // then
        assertThat(result.getScore()).isEqualTo(59); // 실제 계산값
        assertThat(result.getPoliceCount()).isEqualTo(0);
        assertThat(result.getCctvCount()).isEqualTo(50);
        assertThat(result.getStreetlightCount()).isEqualTo(20);
    }

    @Test
    @DisplayName("안전도 분석 - 시설물 없는 경우 최소 점수")
    void analyzeSafety_NoFacilities_MinimumScore() {
        // given
        double lat = 37.5;
        double lon = 127.0;

        when(policeRepository.countNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(0);
        when(cctvRepository.sumQtyNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(0);
        when(streetLightRepository.countInBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(0);

        // when
        SafetyAnalysisResult result = safetyScoringService.analyzeSafety(lat, lon);

        // then
        assertThat(result.getScore()).isEqualTo(55); // 베이스 점수
        assertThat(result.getPoliceCount()).isEqualTo(0);
        assertThat(result.getCctvCount()).isEqualTo(0);
        assertThat(result.getStreetlightCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("안전도 분석 - 경찰서만 많은 경우")
    void analyzeSafety_OnlyPoliceStations_MediumScore() {
        // given
        double lat = 37.5;
        double lon = 127.0;

        when(policeRepository.countNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(3);
        when(cctvRepository.sumQtyNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(0);
        when(streetLightRepository.countInBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(0);

        // when
        SafetyAnalysisResult result = safetyScoringService.analyzeSafety(lat, lon);

        // then
        assertThat(result.getScore()).isEqualTo(69); // 실제 계산값 (normalizedPolice=1.0, 30% 가중치 = 13.5점 추가)
        assertThat(result.getPoliceCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("안전도 분석 - 가로등만 많은 경우")
    void analyzeSafety_OnlyStreetLights_MediumScore() {
        // given
        double lat = 37.5;
        double lon = 127.0;

        when(policeRepository.countNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(0);
        when(cctvRepository.sumQtyNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(0);
        when(streetLightRepository.countInBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(250);

        // when
        SafetyAnalysisResult result = safetyScoringService.analyzeSafety(lat, lon);

        // then
        assertThat(result.getScore()).isEqualTo(73); // 실제 계산값 (normalizedLight=1.0, 40% 가중치 = 18점 추가)
        assertThat(result.getStreetlightCount()).isEqualTo(250);
    }

    @Test
    @DisplayName("안전도 분석 - CCTV null 처리")
    void analyzeSafety_NullCCTV_HandledGracefully() {
        // given
        double lat = 37.5;
        double lon = 127.0;

        when(policeRepository.countNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(1);
        when(cctvRepository.sumQtyNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(null); // null 반환
        when(streetLightRepository.countInBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(100);

        // when
        SafetyAnalysisResult result = safetyScoringService.analyzeSafety(lat, lon);

        // then
        assertThat(result.getScore()).isBetween(0, 100);
        assertThat(result.getCctvCount()).isEqualTo(0); // null은 0으로 처리
    }

    @Test
    @DisplayName("안전도 점수 범위 검증 - 항상 0~100 사이")
    void analyzeSafety_ScoreRange_AlwaysBetween0And100() {
        // given
        double lat = 37.5;
        double lon = 127.0;

        // 극단적으로 많은 시설물
        when(policeRepository.countNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(100);
        when(cctvRepository.sumQtyNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(10000);
        when(streetLightRepository.countInBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(10000);

        // when
        SafetyAnalysisResult result = safetyScoringService.analyzeSafety(lat, lon);

        // then
        assertThat(result.getScore()).isBetween(0, 100);
        assertThat(result.getScore()).isEqualTo(100); // 최대 100점으로 제한
    }

    @Test
    @DisplayName("안전도 분석 - 예외 발생 시 안전한 기본값 반환")
    void analyzeSafety_ExceptionHandling_ReturnsZeroScore() {
        // given
        double lat = 37.5;
        double lon = 127.0;

        when(policeRepository.countNearby(anyDouble(), anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("Database error"));

        // when
        SafetyAnalysisResult result = safetyScoringService.analyzeSafety(lat, lon);

        // then
        assertThat(result.getScore()).isEqualTo(0); // 예외 발생 시 0점 반환
        assertThat(result.getPoliceCount()).isEqualTo(0);
        assertThat(result.getCctvCount()).isEqualTo(0);
        assertThat(result.getStreetlightCount()).isEqualTo(0);
    }
}
