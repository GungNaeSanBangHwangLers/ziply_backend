package ziply.review.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ziply.review.domain.BasePoint;
import ziply.review.domain.House;
import ziply.review.domain.SearchCard;
import ziply.review.dto.request.HouseCreateRequest;
import ziply.review.dto.response.GeocodingResultResponse;
import ziply.review.event.HouseCreatedEvent;
import ziply.review.repository.HouseRepository;
import ziply.review.repository.SearchCardRepository;

@ExtendWith(MockitoExtension.class)
class HouseServiceTest {

    @Mock
    private HouseRepository houseRepository;

    @Mock
    private SearchCardRepository searchCardRepository;

    @Mock
    private ReviewProducerService reviewProducerService;

    @Mock
    private GeocodingService geocodingService;

    @InjectMocks
    private HouseService houseService;

    private SearchCard testCard;
    private UUID testCardId;

    @BeforeEach
    void setUp() {
        testCardId = UUID.randomUUID();
        testCard = new SearchCard(1L, "테스트 카드", null, null);
        ReflectionTestUtils.setField(testCard, "id", testCardId);

        BasePoint basePoint = new BasePoint("기점", "서울시 강남구", 37.5, 127.0);
        ReflectionTestUtils.setField(basePoint, "id", 1L);
        testCard.addBasePoint(basePoint);
        
        // producerService와 reviewProducerService가 같은 Mock을 가리키도록 설정
        ReflectionTestUtils.setField(houseService, "producerService", reviewProducerService);
    }

    @Test
    @DisplayName("집 생성 - 정상 생성")
    void createHouses_Success() {
        // given
        HouseCreateRequest request1 = new HouseCreateRequest();
        ReflectionTestUtils.setField(request1, "address", "서울시 서초구");
        ReflectionTestUtils.setField(request1, "visitDateTime", LocalDateTime.now());

        HouseCreateRequest request2 = new HouseCreateRequest();
        ReflectionTestUtils.setField(request2, "address", "서울시 강남구");
        ReflectionTestUtils.setField(request2, "visitDateTime", LocalDateTime.now());

        List<HouseCreateRequest> requests = List.of(request1, request2);

        when(searchCardRepository.findById(testCardId)).thenReturn(Optional.of(testCard));
        when(houseRepository.countBySearchCard(testCard)).thenReturn(0L);

        doAnswer(invocation -> {
            HouseCreateRequest req = invocation.getArgument(0);
            if (req.getAddress().equals("서울시 서초구")) {
                ReflectionTestUtils.setField(req, "latitude", 37.48);
                ReflectionTestUtils.setField(req, "longitude", 127.02);
            } else {
                ReflectionTestUtils.setField(req, "latitude", 37.5);
                ReflectionTestUtils.setField(req, "longitude", 127.0);
            }
            return null;
        }).when(geocodingService).geocodeAddress(any(HouseCreateRequest.class));

        House savedHouse1 = House.builder()
                .id(1L)
                .searchCard(testCard)
                .address("서울시 서초구")
                .latitude(37.48)
                .longitude(127.02)
                .build();

        House savedHouse2 = House.builder()
                .id(2L)
                .searchCard(testCard)
                .address("서울시 강남구")
                .latitude(37.5)
                .longitude(127.0)
                .build();

        when(houseRepository.saveAll(anyList())).thenReturn(List.of(savedHouse1, savedHouse2));

        // when
        List<Long> result = houseService.createHouses(testCardId, requests, 1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(1L, 2L);
        verify(houseRepository).saveAll(anyList());
        verify(reviewProducerService, times(2)).sendHouseCreatedEvent(any(HouseCreatedEvent.class));
    }

    @Test
    @DisplayName("집 생성 - 권한 없는 사용자")
    void createHouses_UnauthorizedUser_ThrowsException() {
        // given
        HouseCreateRequest request = new HouseCreateRequest();
        ReflectionTestUtils.setField(request, "address", "서울시 서초구");
        ReflectionTestUtils.setField(request, "visitDateTime", LocalDateTime.now());

        when(searchCardRepository.findById(testCardId)).thenReturn(Optional.of(testCard));

        // when & then
        assertThatThrownBy(() -> houseService.createHouses(testCardId, List.of(request), 999L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("본인의 탐색 카드에만 집을 추가할 수 있습니다");
    }

    @Test
    @DisplayName("집 생성 - 최대 개수 초과")
    void createHouses_ExceedsMaxLimit_ThrowsException() {
        // given
        HouseCreateRequest request = new HouseCreateRequest();
        ReflectionTestUtils.setField(request, "address", "서울시 서초구");
        ReflectionTestUtils.setField(request, "visitDateTime", LocalDateTime.now());

        when(searchCardRepository.findById(testCardId)).thenReturn(Optional.of(testCard));
        when(houseRepository.countBySearchCard(testCard)).thenReturn(7L);

        // when & then
        assertThatThrownBy(() -> houseService.createHouses(testCardId, List.of(request), 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("한 카드당 집은 최대 7개까지 등록할 수 있습니다");
    }

    @Test
    @DisplayName("집 생성 - 일부 지오코딩 실패 시 성공한 것만 저장")
    void createHouses_PartialGeocodingFailure_SavesOnlySuccessful() {
        // given
        HouseCreateRequest request1 = new HouseCreateRequest();
        ReflectionTestUtils.setField(request1, "address", "잘못된 주소");
        ReflectionTestUtils.setField(request1, "visitDateTime", LocalDateTime.now());

        HouseCreateRequest request2 = new HouseCreateRequest();
        ReflectionTestUtils.setField(request2, "address", "서울시 강남구");
        ReflectionTestUtils.setField(request2, "visitDateTime", LocalDateTime.now());

        when(searchCardRepository.findById(testCardId)).thenReturn(Optional.of(testCard));
        when(houseRepository.countBySearchCard(testCard)).thenReturn(0L);

        doAnswer(invocation -> {
            HouseCreateRequest req = invocation.getArgument(0);
            if (req.getAddress().equals("잘못된 주소")) {
                throw new RuntimeException("Geocoding failed");
            } else {
                ReflectionTestUtils.setField(req, "latitude", 37.5);
                ReflectionTestUtils.setField(req, "longitude", 127.0);
            }
            return null;
        }).when(geocodingService).geocodeAddress(any(HouseCreateRequest.class));

        House savedHouse = House.builder()
                .id(1L)
                .searchCard(testCard)
                .address("서울시 강남구")
                .latitude(37.5)
                .longitude(127.0)
                .build();

        when(houseRepository.saveAll(anyList())).thenReturn(List.of(savedHouse));

        // when
        List<Long> result = houseService.createHouses(testCardId, List.of(request1, request2), 1L);

        // then
        assertThat(result).hasSize(1);
        ArgumentCaptor<List<House>> captor = ArgumentCaptor.forClass(List.class);
        verify(houseRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("집 삭제 - 정상 삭제 (카드에 다른 집이 남아있는 경우)")
    void deleteHouse_Success() {
        // given
        House house = House.builder()
                .id(1L)
                .searchCard(testCard)
                .address("서울시 서초구")
                .build();

        when(houseRepository.findById(1L)).thenReturn(Optional.of(house));
        when(houseRepository.countBySearchCard(testCard)).thenReturn(1L); // 삭제 후에도 1개 남음

        // when
        houseService.deleteHouse(1L, 1L);

        // then
        verify(houseRepository).delete(house);
        verify(houseRepository).countBySearchCard(testCard);
        verify(searchCardRepository, never()).delete(any()); // 카드는 삭제되지 않음
        verify(reviewProducerService, never()).sendCardDeletedEvent(any());
        verify(reviewProducerService).sendDeleteSignal(1L); // 집 삭제 시그널은 항상 발송
    }

    @Test
    @DisplayName("집 삭제 - 마지막 집 삭제 시 카드도 삭제")
    void deleteHouse_LastHouse_DeletesCard() {
        // given
        House house = House.builder()
                .id(1L)
                .searchCard(testCard)
                .address("서울시 서초구")
                .build();

        when(houseRepository.findById(1L)).thenReturn(Optional.of(house));
        when(houseRepository.countBySearchCard(testCard)).thenReturn(0L); // 삭제 후 0개 남음

        // when
        houseService.deleteHouse(1L, 1L);

        // then
        verify(houseRepository).delete(house);
        verify(houseRepository).countBySearchCard(testCard);
        verify(searchCardRepository).delete(testCard); // 카드도 삭제
        verify(reviewProducerService).sendCardDeletedEvent(testCardId); // 카드 삭제 이벤트
        verify(reviewProducerService).sendDeleteSignal(1L); // 집 삭제 시그널도 발송
    }

    @Test
    @DisplayName("집 삭제 - 권한 없는 사용자")
    void deleteHouse_UnauthorizedUser_ThrowsException() {
        // given
        House house = House.builder()
                .id(1L)
                .searchCard(testCard)
                .address("서울시 서초구")
                .build();

        when(houseRepository.findById(1L)).thenReturn(Optional.of(house));

        // when & then
        assertThatThrownBy(() -> houseService.deleteHouse(1L, 999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("권한 없음");
    }

    @Test
    @DisplayName("집 삭제 - 존재하지 않는 집")
    void deleteHouse_NonExistentHouse_ThrowsException() {
        // given
        when(houseRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> houseService.deleteHouse(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("집 없음");
    }

    @Test
    @DisplayName("탐색 카드별 집 조회 - 정상 조회")
    void getHousesBySearchCard_Success() {
        // given
        House house1 = House.builder()
                .id(1L)
                .searchCard(testCard)
                .address("서울시 서초구")
                .visitDateTime(LocalDateTime.now())
                .build();

        House house2 = House.builder()
                .id(2L)
                .searchCard(testCard)
                .address("서울시 강남구")
                .visitDateTime(LocalDateTime.now().plusDays(1))
                .build();

        when(searchCardRepository.findByIdAndUserId(testCardId, 1L)).thenReturn(Optional.of(testCard));
        when(houseRepository.findBySearchCardId(testCardId)).thenReturn(List.of(house1, house2));

        // when
        var result = houseService.getHousesBySearchCard(testCardId, 1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLabel()).isEqualTo("A");
        assertThat(result.get(1).getLabel()).isEqualTo("B");
    }

    @Test
    @DisplayName("탐색 카드별 집 조회 - 권한 없음")
    void getHousesBySearchCard_Unauthorized_ThrowsException() {
        // given
        when(searchCardRepository.findByIdAndUserId(testCardId, 999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> houseService.getHousesBySearchCard(testCardId, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("접근 권한이 없거나 존재하지 않는 카드입니다");
    }
}
