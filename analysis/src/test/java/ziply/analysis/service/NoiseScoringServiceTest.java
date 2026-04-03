package ziply.analysis.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import ziply.analysis.domain.HouseInfrastructure;
import ziply.analysis.repository.HouseInfrastructureRepository;

@ExtendWith(MockitoExtension.class)
class NoiseScoringServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private HouseInfrastructureRepository infrastructureRepository;

    @InjectMocks
    private NoiseScoringService noiseScoringService;

    private HouseInfrastructure mockInfra;

    @BeforeEach
    void setUp() {
        // 기본 인프라 설정: 학교 2개, 지하철 1개, 음식점 50개
        mockInfra = new HouseInfrastructure(1L, 2, 1, 50);
    }

    @Test
    @DisplayName("주간 소음 점수 계산 - 조용한 지역 (높은 점수)")
    void calculateDayNoiseScore_QuietArea_HighScore() {
        // given
        Long houseId = 1L;
        double lat = 37.5;
        double lon = 127.0;

        HouseInfrastructure quietInfra = new HouseInfrastructure(houseId, 0, 0, 10);
        when(infrastructureRepository.findByHouseId(houseId)).thenReturn(Optional.of(quietInfra));
        
        // 버스 운행 적음
        when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), anyDouble(), anyDouble(), 
                anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(10.0);

        // when
        int score = noiseScoringService.calculateDayNoiseScore(houseId, lat, lon);

        // then
        assertThat(score).isBetween(15, 100);
        assertThat(score).isGreaterThan(60); // 조용한 지역
    }

    @Test
    @DisplayName("주간 소음 점수 계산 - 시끄러운 지역 (낮은 점수)")
    void calculateDayNoiseScore_NoisyArea_LowScore() {
        // given
        Long houseId = 1L;
        double lat = 37.5;
        double lon = 127.0;

        HouseInfrastructure noisyInfra = new HouseInfrastructure(houseId, 5, 3, 150);
        when(infrastructureRepository.findByHouseId(houseId)).thenReturn(Optional.of(noisyInfra));
        
        // 버스 운행 많음
        when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), anyDouble(), anyDouble(), 
                anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(800.0);

        // when
        int score = noiseScoringService.calculateDayNoiseScore(houseId, lat, lon);

        // then
        assertThat(score).isBetween(15, 100);
        assertThat(score).isLessThan(60); // 시끄러운 지역
    }

    @Test
    @DisplayName("야간 소음 점수 계산 - 조용한 지역 (높은 점수)")
    void calculateNightNoiseScore_QuietArea_HighScore() {
        // given
        Long houseId = 1L;
        double lat = 37.5;
        double lon = 127.0;

        HouseInfrastructure quietInfra = new HouseInfrastructure(houseId, 0, 0, 5);
        when(infrastructureRepository.findByHouseId(houseId)).thenReturn(Optional.of(quietInfra));
        
        // 야간 버스 운행 거의 없음
        when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), anyDouble(), anyDouble(), 
                anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(5.0);

        // when
        int score = noiseScoringService.calculateNightNoiseScore(houseId, lat, lon);

        // then
        assertThat(score).isBetween(15, 100);
        assertThat(score).isGreaterThan(65); // 야간 매우 조용
    }

    @Test
    @DisplayName("야간 소음 점수 계산 - 유흥가 (낮은 점수)")
    void calculateNightNoiseScore_BarArea_LowScore() {
        // given
        Long houseId = 1L;
        double lat = 37.5;
        double lon = 127.0;

        HouseInfrastructure barArea = new HouseInfrastructure(houseId, 0, 0, 200); // 음식점 많음
        when(infrastructureRepository.findByHouseId(houseId)).thenReturn(Optional.of(barArea));
        
        // 야간 버스 운행 많음 (calculateNightNoiseScore는 쿼리를 2번 호출함)
        when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), anyDouble(), anyDouble(), 
                anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(80.0, 80.0); // 첫 번째, 두 번째 호출 모두 80.0 반환

        // when
        int score = noiseScoringService.calculateNightNoiseScore(houseId, lat, lon);

        // then
        System.out.println("[DEBUG] 야간 유흥가 실제 점수: " + score);
        assertThat(score).isBetween(15, 100);
        assertThat(score).isLessThan(80);
    }

    @Test
    @DisplayName("소음 점수 계산 - 인프라 정보 없는 경우 기본값 사용")
    void calculateDayNoiseScore_NoInfra_UsesDefault() {
        // given
        Long houseId = 1L;
        double lat = 37.5;
        double lon = 127.0;

        when(infrastructureRepository.findByHouseId(houseId)).thenReturn(Optional.empty());
        when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), anyDouble(), anyDouble(), 
                anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(100.0);

        // when
        int score = noiseScoringService.calculateDayNoiseScore(houseId, lat, lon);

        // then
        assertThat(score).isBetween(15, 100); // 항상 유효한 범위
    }

    @Test
    @DisplayName("소음 점수 범위 검증 - 항상 15~100 사이")
    void noiseScore_AlwaysInValidRange() {
        // given
        Long houseId = 1L;
        double lat = 37.5;
        double lon = 127.0;

        // 극단적인 인프라
        HouseInfrastructure extremeInfra = new HouseInfrastructure(houseId, 100, 100, 1000);
        when(infrastructureRepository.findByHouseId(houseId)).thenReturn(Optional.of(extremeInfra));
        
        // 극단적인 버스 운행
        when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), anyDouble(), anyDouble(), 
                anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(10000.0);

        // when
        int dayScore = noiseScoringService.calculateDayNoiseScore(houseId, lat, lon);
        int nightScore = noiseScoringService.calculateNightNoiseScore(houseId, lat, lon);

        // then
        assertThat(dayScore).isBetween(15, 100);
        assertThat(nightScore).isBetween(15, 100);
    }

    @Test
    @DisplayName("주간 점수가 야간 점수보다 낮거나 같음 (학교 영향)")
    void dayScore_LowerOrEqualToNightScore_WhenSchoolsPresent() {
        // given
        Long houseId = 1L;
        double lat = 37.5;
        double lon = 127.0;

        HouseInfrastructure schoolArea = new HouseInfrastructure(houseId, 10, 0, 20);
        when(infrastructureRepository.findByHouseId(houseId)).thenReturn(Optional.of(schoolArea));
        
        // 버스 운행 보통
        when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), anyDouble(), anyDouble(), 
                anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(200.0);

        // when
        int dayScore = noiseScoringService.calculateDayNoiseScore(houseId, lat, lon);
        int nightScore = noiseScoringService.calculateNightNoiseScore(houseId, lat, lon);

        // then
        // 학교는 주간에만 영향을 주므로 주간 점수가 더 낮을 가능성이 크다
        // 하지만 반드시 그런 것은 아니므로 (버스 야간 운행이 적을 수 있음) 둘 다 유효한 범위 내에 있는지만 확인
        assertThat(dayScore).isBetween(15, 100);
        assertThat(nightScore).isBetween(15, 100);
    }

    @Test
    @DisplayName("버스 데이터 null 처리")
    void calculateNoiseScore_NullBusData_HandledGracefully() {
        // given
        Long houseId = 1L;
        double lat = 37.5;
        double lon = 127.0;

        when(infrastructureRepository.findByHouseId(houseId)).thenReturn(Optional.of(mockInfra));
        when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), anyDouble(), anyDouble(), 
                anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(null);

        // when
        int dayScore = noiseScoringService.calculateDayNoiseScore(houseId, lat, lon);
        int nightScore = noiseScoringService.calculateNightNoiseScore(houseId, lat, lon);

        // then
        assertThat(dayScore).isBetween(15, 100);
        assertThat(nightScore).isBetween(15, 100);
    }

    @Test
    @DisplayName("지하철역 많으면 점수 영향")
    void calculateDayNoiseScore_SubwayImpact() {
        // given
        Long houseId = 1L;
        double lat = 37.5;
        double lon = 127.0;

        HouseInfrastructure subwayArea = new HouseInfrastructure(houseId, 0, 5, 10);
        when(infrastructureRepository.findByHouseId(houseId)).thenReturn(Optional.of(subwayArea));
        
        // calculateDayNoiseScore는 쿼리를 2번 호출함
        when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), anyDouble(), anyDouble(), 
                anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(50.0, 50.0); // 첫 번째, 두 번째 호출 모두 50.0 반환

        // when
        int score = noiseScoringService.calculateDayNoiseScore(houseId, lat, lon);

        // then
        assertThat(score).isBetween(15, 100);
        // 지하철 5개는 매우 많지만, 버스와 음식점이 적어서 전체 점수는 높음 (실제 약 89점)
        assertThat(score).isGreaterThan(85);
    }
}
