package ziply.analysis.service;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ziply.analysis.repository.HouseInfrastructureRepository;
import ziply.analysis.repository.HouseRouteAnalysisRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("DemoAnalysisService 테스트")
class DemoAnalysisServiceTest {

    @Mock
    private HouseRouteAnalysisRepository houseRouteAnalysisRepository;

    @Mock
    private HouseInfrastructureRepository houseInfrastructureRepository;

    @InjectMocks
    private DemoAnalysisService demoAnalysisService;

    private List<Long> houseIds;

    @BeforeEach
    void setUp() {
        houseIds = Arrays.asList(1L, 2L, 3L);
    }

    @Test
    @DisplayName("데모 분석 데이터 초기화 성공")
    void resetDemoAnalysisData_Success() {
        // when
        demoAnalysisService.resetDemoAnalysisData(houseIds);

        // then
        verify(houseRouteAnalysisRepository).deleteByHouseId(1L);
        verify(houseRouteAnalysisRepository).deleteByHouseId(2L);
        verify(houseRouteAnalysisRepository).deleteByHouseId(3L);
        
        verify(houseInfrastructureRepository).deleteByHouseId(1L);
        verify(houseInfrastructureRepository).deleteByHouseId(2L);
        verify(houseInfrastructureRepository).deleteByHouseId(3L);
    }

    @Test
    @DisplayName("빈 houseIds 리스트 처리")
    void resetDemoAnalysisData_EmptyList() {
        // when
        demoAnalysisService.resetDemoAnalysisData(Collections.emptyList());

        // then
        verify(houseRouteAnalysisRepository, never()).deleteByHouseId(anyLong());
        verify(houseInfrastructureRepository, never()).deleteByHouseId(anyLong());
    }

    @Test
    @DisplayName("null houseIds 처리")
    void resetDemoAnalysisData_NullList() {
        // when
        demoAnalysisService.resetDemoAnalysisData(null);

        // then
        verify(houseRouteAnalysisRepository, never()).deleteByHouseId(anyLong());
        verify(houseInfrastructureRepository, never()).deleteByHouseId(anyLong());
    }

    @Test
    @DisplayName("단일 houseId 처리")
    void resetDemoAnalysisData_SingleHouse() {
        // given
        List<Long> singleHouseId = List.of(10L);

        // when
        demoAnalysisService.resetDemoAnalysisData(singleHouseId);

        // then
        verify(houseRouteAnalysisRepository).deleteByHouseId(10L);
        verify(houseInfrastructureRepository).deleteByHouseId(10L);
        
        verify(houseRouteAnalysisRepository, times(1)).deleteByHouseId(anyLong());
        verify(houseInfrastructureRepository, times(1)).deleteByHouseId(anyLong());
    }

    @Test
    @DisplayName("다수의 houseIds 처리")
    void resetDemoAnalysisData_ManyHouses() {
        // given
        List<Long> manyHouseIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);

        // when
        demoAnalysisService.resetDemoAnalysisData(manyHouseIds);

        // then
        verify(houseRouteAnalysisRepository, times(10)).deleteByHouseId(anyLong());
        verify(houseInfrastructureRepository, times(10)).deleteByHouseId(anyLong());
    }
}
