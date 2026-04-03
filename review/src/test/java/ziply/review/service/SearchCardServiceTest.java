package ziply.review.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
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
import ziply.review.dto.request.SearchCardCreateRequest;
import ziply.review.dto.response.GeocodingResultResponse;
import ziply.review.dto.response.SearchCardResponse;
import ziply.review.event.HouseCreatedEvent;
import ziply.review.repository.BasePointRepository;
import ziply.review.repository.HouseRepository;
import ziply.review.repository.SearchCardRepository;

@ExtendWith(MockitoExtension.class)
class SearchCardServiceTest {

    @Mock
    private SearchCardRepository searchCardRepository;

    @Mock
    private GeocodingService geocodingService;

    @Mock
    private HouseRepository houseRepository;

    @Mock
    private ReviewProducerService reviewProducerService;

    @Mock
    private BasePointRepository basePointRepository;

    @InjectMocks
    private SearchCardService searchCardService;

    private SearchCard testCard;
    private UUID testCardId;

    @BeforeEach
    void setUp() {
        testCardId = UUID.randomUUID();
        testCard = new SearchCard(1L, "테스트 카드", LocalDate.now(), LocalDate.now().plusDays(7));
        ReflectionTestUtils.setField(testCard, "id", testCardId);
    }

    @Test
    @DisplayName("탐색 카드 생성 - 집 없이 기점만 있는 경우")
    void createSearchCard_WithoutHouses_WithBasePoint() {
        // given
        SearchCardCreateRequest request = new SearchCardCreateRequest();
        ReflectionTestUtils.setField(request, "basePointAddress", "서울시 강남구");
        ReflectionTestUtils.setField(request, "houses", new ArrayList<>());

        GeocodingResultResponse geocodingResult = new GeocodingResultResponse(37.5, 127.0);
        when(geocodingService.geocodeAddress("서울시 강남구")).thenReturn(geocodingResult);
        when(searchCardRepository.save(any(SearchCard.class))).thenReturn(testCard);

        // when
        UUID result = searchCardService.createSearchCard(1L, request);

        // then
        assertThat(result).isEqualTo(testCardId);
        verify(geocodingService).geocodeAddress("서울시 강남구");
        verify(searchCardRepository).save(any(SearchCard.class));
        verify(houseRepository).saveAll(anyList());
        verify(reviewProducerService, never()).sendHouseCreatedEvent(any());
    }

    @Test
    @DisplayName("탐색 카드 생성 - 집과 기점 모두 있는 경우")
    void createSearchCard_WithHousesAndBasePoint() {
        // given
        SearchCardCreateRequest.HouseCreateRequest houseReq = new SearchCardCreateRequest.HouseCreateRequest();
        ReflectionTestUtils.setField(houseReq, "address", "서울시 서초구");
        ReflectionTestUtils.setField(houseReq, "visitDateTime", LocalDateTime.now());

        SearchCardCreateRequest request = new SearchCardCreateRequest();
        ReflectionTestUtils.setField(request, "basePointAddress", "서울시 강남구");
        ReflectionTestUtils.setField(request, "houses", List.of(houseReq));

        GeocodingResultResponse baseGeo = new GeocodingResultResponse(37.5, 127.0);
        GeocodingResultResponse houseGeo = new GeocodingResultResponse(37.48, 127.02);

        when(geocodingService.geocodeAddress("서울시 강남구")).thenReturn(baseGeo);
        when(geocodingService.geocodeAddress("서울시 서초구")).thenReturn(houseGeo);
        when(searchCardRepository.save(any(SearchCard.class))).thenReturn(testCard);

        House savedHouse = House.builder()
                .searchCard(testCard)
                .address("서울시 서초구")
                .latitude(37.48)
                .longitude(127.02)
                .build();
        when(houseRepository.saveAll(anyList())).thenReturn(List.of(savedHouse));

        // when
        UUID result = searchCardService.createSearchCard(1L, request);

        // then
        assertThat(result).isEqualTo(testCardId);
        verify(geocodingService).geocodeAddress("서울시 강남구");
        verify(geocodingService).geocodeAddress("서울시 서초구");
        verify(houseRepository).saveAll(anyList());
        verify(reviewProducerService).sendHouseCreatedEvent(any(HouseCreatedEvent.class));
    }

    @Test
    @DisplayName("탐색 카드 생성 - 집 지오코딩 실패 시 해당 집만 제외하고 생성")
    void createSearchCard_WithGeocodingFailure_SkipsFailedHouse() {
        // given
        SearchCardCreateRequest.HouseCreateRequest houseReq1 = new SearchCardCreateRequest.HouseCreateRequest();
        ReflectionTestUtils.setField(houseReq1, "address", "잘못된 주소");
        ReflectionTestUtils.setField(houseReq1, "visitDateTime", LocalDateTime.now());

        SearchCardCreateRequest.HouseCreateRequest houseReq2 = new SearchCardCreateRequest.HouseCreateRequest();
        ReflectionTestUtils.setField(houseReq2, "address", "서울시 서초구");
        ReflectionTestUtils.setField(houseReq2, "visitDateTime", LocalDateTime.now());

        SearchCardCreateRequest request = new SearchCardCreateRequest();
        ReflectionTestUtils.setField(request, "basePointAddress", "서울시 강남구");
        ReflectionTestUtils.setField(request, "houses", List.of(houseReq1, houseReq2));

        GeocodingResultResponse baseGeo = new GeocodingResultResponse(37.5, 127.0);
        GeocodingResultResponse houseGeo = new GeocodingResultResponse(37.48, 127.02);

        when(geocodingService.geocodeAddress("서울시 강남구")).thenReturn(baseGeo);
        when(geocodingService.geocodeAddress("잘못된 주소")).thenThrow(new RuntimeException("Geocoding failed"));
        when(geocodingService.geocodeAddress("서울시 서초구")).thenReturn(houseGeo);
        when(searchCardRepository.save(any(SearchCard.class))).thenReturn(testCard);

        House savedHouse = House.builder()
                .searchCard(testCard)
                .address("서울시 서초구")
                .latitude(37.48)
                .longitude(127.02)
                .build();
        when(houseRepository.saveAll(anyList())).thenReturn(List.of(savedHouse));

        // when
        UUID result = searchCardService.createSearchCard(1L, request);

        // then
        assertThat(result).isEqualTo(testCardId);
        ArgumentCaptor<List<House>> captor = ArgumentCaptor.forClass(List.class);
        verify(houseRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getAddress()).isEqualTo("서울시 서초구");
    }

    @Test
    @DisplayName("유저의 모든 탐색 카드 조회")
    void getSearchCards_ReturnsAllUserCards() {
        // given
        Long userId = 1L;
        SearchCard card1 = new SearchCard(userId, "카드1", LocalDate.now(), LocalDate.now().plusDays(7));
        SearchCard card2 = new SearchCard(userId, "카드2", LocalDate.now(), LocalDate.now().plusDays(14));

        when(searchCardRepository.findAllByUserId(userId)).thenReturn(List.of(card1, card2));

        // when
        List<SearchCardResponse> result = searchCardService.getSearchCards(userId);

        // then
        assertThat(result).hasSize(2);
        verify(searchCardRepository).findAllByUserId(userId);
    }

    @Test
    @DisplayName("카드 소유권 확인 - 소유자인 경우")
    void isCardOwner_WhenUserOwnsCard_ReturnsTrue() {
        // given
        UUID cardId = UUID.randomUUID();
        Long userId = 1L;
        when(searchCardRepository.existsByIdAndUserId(cardId, userId)).thenReturn(true);

        // when
        boolean result = searchCardService.isCardOwner(cardId, userId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("카드 소유권 확인 - 소유자가 아닌 경우")
    void isCardOwner_WhenUserDoesNotOwnCard_ReturnsFalse() {
        // given
        UUID cardId = UUID.randomUUID();
        Long userId = 1L;
        when(searchCardRepository.existsByIdAndUserId(cardId, userId)).thenReturn(false);

        // when
        boolean result = searchCardService.isCardOwner(cardId, userId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("기점 주소 조회 - 존재하는 카드")
    void getBasePointAddresses_WithValidCard_ReturnsAddresses() {
        // given
        BasePoint basePoint = new BasePoint("기점", "서울시 강남구", 37.5, 127.0);
        testCard.addBasePoint(basePoint);

        when(searchCardRepository.findById(testCardId)).thenReturn(Optional.of(testCard));

        // when
        var result = searchCardService.getBasePointAddresses(testCardId, 1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).address()).isEqualTo("서울시 강남구");
    }

    @Test
    @DisplayName("기점 주소 조회 - 존재하지 않는 카드")
    void getBasePointAddresses_WithInvalidCard_ThrowsException() {
        // given
        when(searchCardRepository.findById(testCardId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> searchCardService.getBasePointAddresses(testCardId, 1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("카드를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("기점 주소 조회 - 권한 없는 사용자")
    void getBasePointAddresses_WithUnauthorizedUser_ThrowsException() {
        // given
        when(searchCardRepository.findById(testCardId)).thenReturn(Optional.of(testCard));

        // when & then
        assertThatThrownBy(() -> searchCardService.getBasePointAddresses(testCardId, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("접근 권한이 없습니다");
    }
}
