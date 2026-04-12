package ziply.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ziply.review.domain.House;
import ziply.review.domain.HouseStatus;
import ziply.review.domain.SearchCard;
import ziply.review.dto.response.DemoResetResponse;
import ziply.review.event.DemoDataResetEvent;
import ziply.review.repository.HouseImageRepository;
import ziply.review.repository.HouseRepository;
import ziply.review.repository.MeasurementRepository;
import ziply.review.repository.SearchCardRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("DemoService 테스트")
class DemoServiceTest {

    @Mock
    private SearchCardRepository searchCardRepository;

    @Mock
    private HouseRepository houseRepository;

    @Mock
    private MeasurementRepository measurementRepository;

    @Mock
    private HouseImageRepository houseImageRepository;

    @Mock
    private ReviewProducerService reviewProducerService;

    @InjectMocks
    private DemoService demoService;

    private Long userId;
    private SearchCard searchCard;
    private House house1;
    private House house2;
    private UUID searchCardId;

    @BeforeEach
    void setUp() {
        userId = 999L;
        searchCardId = UUID.randomUUID();
        
        // SearchCard 생성 (생성자 사용)
        searchCard = new SearchCard(userId, "테스트 카드", LocalDate.now(), LocalDate.now().plusDays(7));
        ReflectionTestUtils.setField(searchCard, "id", searchCardId);

        // House 생성 (Builder 사용)
        house1 = House.builder()
                .searchCard(searchCard)
                .address("서울시 강남구")
                .latitude(37.5)
                .longitude(127.0)
                .status(HouseStatus.AFTER)
                .build();
        ReflectionTestUtils.setField(house1, "id", 1L);

        house2 = House.builder()
                .searchCard(searchCard)
                .address("서울시 서초구")
                .latitude(37.4)
                .longitude(127.1)
                .status(HouseStatus.IN_PROGRESS)
                .build();
        ReflectionTestUtils.setField(house2, "id", 2L);
    }

    @Test
    @DisplayName("데모 데이터 초기화 성공 - 데이터 있음")
    void resetDemoData_Success() {
        // given
        when(searchCardRepository.findAllByUserId(userId))
                .thenReturn(List.of(searchCard));
        
        when(houseRepository.findBySearchCardId(searchCardId))
                .thenReturn(List.of(house1, house2));
        
        when(measurementRepository.countByHouseId(1L)).thenReturn(2);
        when(measurementRepository.countByHouseId(2L)).thenReturn(1);
        
        when(houseImageRepository.findAllByHouseId(1L))
                .thenReturn(Collections.emptyList());
        when(houseImageRepository.findAllByHouseId(2L))
                .thenReturn(Collections.emptyList());

        // when
        DemoResetResponse response = demoService.resetDemoData(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("데모 데이터가 초기화되었습니다.");
        assertThat(response.getDeletedData().getSearchCards()).isEqualTo(1);
        assertThat(response.getDeletedData().getHouses()).isEqualTo(2);
        assertThat(response.getDeletedData().getMeasurements()).isEqualTo(3);
        assertThat(response.getDeletedData().getImages()).isEqualTo(0);

        // verify 삭제 호출
        verify(measurementRepository).deleteAllByHouseId(1L);
        verify(measurementRepository).deleteAllByHouseId(2L);
        verify(houseImageRepository).deleteAllByHouseId(1L);
        verify(houseImageRepository).deleteAllByHouseId(2L);
        verify(houseRepository).deleteAll(any());
        verify(searchCardRepository).deleteAll(any());
        
        // verify Kafka 이벤트 발행
        verify(reviewProducerService).sendDemoDataResetEvent(any(DemoDataResetEvent.class));
    }

    @Test
    @DisplayName("데모 데이터 초기화 - 삭제할 데이터 없음")
    void resetDemoData_NoData() {
        // given
        when(searchCardRepository.findAllByUserId(userId))
                .thenReturn(Collections.emptyList());

        // when
        DemoResetResponse response = demoService.resetDemoData(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("삭제할 데이터가 없습니다.");
        assertThat(response.getDeletedData().getSearchCards()).isEqualTo(0);
        assertThat(response.getDeletedData().getHouses()).isEqualTo(0);
        assertThat(response.getDeletedData().getMeasurements()).isEqualTo(0);
        assertThat(response.getDeletedData().getImages()).isEqualTo(0);

        // verify 삭제 호출되지 않음
        verify(houseRepository, never()).deleteAll(any());
        verify(searchCardRepository, never()).deleteAll(any());
        verify(reviewProducerService, never()).sendDemoDataResetEvent(any());
    }

    @Test
    @DisplayName("데모 데이터 초기화 - SearchCard는 있지만 House가 없음")
    void resetDemoData_NoHouses() {
        // given
        when(searchCardRepository.findAllByUserId(userId))
                .thenReturn(List.of(searchCard));
        
        when(houseRepository.findBySearchCardId(searchCardId))
                .thenReturn(Collections.emptyList());

        // when
        DemoResetResponse response = demoService.resetDemoData(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("데모 데이터가 초기화되었습니다.");
        assertThat(response.getDeletedData().getSearchCards()).isEqualTo(1);
        assertThat(response.getDeletedData().getHouses()).isEqualTo(0);
        assertThat(response.getDeletedData().getMeasurements()).isEqualTo(0);
        assertThat(response.getDeletedData().getImages()).isEqualTo(0);

        // verify SearchCard 삭제
        verify(searchCardRepository).deleteAll(any());
        
        // verify Kafka 이벤트 발행 (빈 houseIds)
        verify(reviewProducerService).sendDemoDataResetEvent(any(DemoDataResetEvent.class));
    }

    @Test
    @DisplayName("데모 데이터 초기화 - 여러 SearchCard와 House")
    void resetDemoData_MultipleCards() {
        // given
        UUID searchCardId2 = UUID.randomUUID();
        SearchCard searchCard2 = new SearchCard(userId, "테스트 카드2", LocalDate.now(), LocalDate.now().plusDays(7));
        ReflectionTestUtils.setField(searchCard2, "id", searchCardId2);

        House house3 = House.builder()
                .searchCard(searchCard2)
                .address("서울시 송파구")
                .latitude(37.5)
                .longitude(127.1)
                .status(HouseStatus.BEFORE)
                .build();
        ReflectionTestUtils.setField(house3, "id", 3L);

        when(searchCardRepository.findAllByUserId(userId))
                .thenReturn(List.of(searchCard, searchCard2));
        
        when(houseRepository.findBySearchCardId(searchCardId))
                .thenReturn(List.of(house1, house2));
        
        when(houseRepository.findBySearchCardId(searchCardId2))
                .thenReturn(List.of(house3));
        
        when(measurementRepository.countByHouseId(anyLong())).thenReturn(1);
        when(houseImageRepository.findAllByHouseId(anyLong())).thenReturn(Collections.emptyList());

        // when
        DemoResetResponse response = demoService.resetDemoData(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDeletedData().getSearchCards()).isEqualTo(2);
        assertThat(response.getDeletedData().getHouses()).isEqualTo(3);
        assertThat(response.getDeletedData().getMeasurements()).isEqualTo(3);

        // verify 모든 House에 대해 삭제 호출
        verify(measurementRepository).deleteAllByHouseId(1L);
        verify(measurementRepository).deleteAllByHouseId(2L);
        verify(measurementRepository).deleteAllByHouseId(3L);
    }
}
