package ziply.review.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ziply.review.domain.House;
import ziply.review.domain.SearchCard;
import ziply.review.dto.request.HouseUpdateRequest;
import ziply.review.dto.response.GeocodingResultResponse;
import ziply.review.repository.HouseRepository;
import ziply.review.repository.SearchCardRepository;

@ExtendWith(MockitoExtension.class)
class HouseServiceUpdateTest {

    @Mock private HouseRepository houseRepository;
    @Mock private SearchCardRepository searchCardRepository;
    @Mock private ReviewProducerService producerService;
    @Mock private GeocodingService geocodingService;
    @Mock private ReviewProducerService reviewProducerService;

    @InjectMocks
    private HouseService houseService;

    @Test
    void updateHouseDoesNotPublishEventWhenAddressIsUnchanged() {
        UUID cardId = UUID.randomUUID();
        SearchCard card = new SearchCard(1L, "카드", null, null);
        ReflectionTestUtils.setField(card, "id", cardId);
        House house = House.builder()
                .id(10L)
                .searchCard(card)
                .address("서울시 강남구")
                .visitDateTime(LocalDateTime.of(2026, 4, 1, 10, 0))
                .build();

        when(houseRepository.findByIdAndSearchCardUserId(10L, 1L)).thenReturn(Optional.of(house));

        HouseUpdateRequest req = new HouseUpdateRequest();
        ReflectionTestUtils.setField(req, "address", "서울시 강남구");
        ReflectionTestUtils.setField(req, "visitDateTime", LocalDateTime.of(2026, 4, 2, 9, 0));

        houseService.updateHouse(10L, 1L, req);

        verify(geocodingService, never()).geocodeAddress(anyString());
        verify(reviewProducerService, never()).sendHouseUpdatedEvent(any());
    }

    @Test
    void updateHousePublishesEventWhenAddressChanged() {
        UUID cardId = UUID.randomUUID();
        SearchCard card = new SearchCard(1L, "카드", null, null);
        ReflectionTestUtils.setField(card, "id", cardId);
        House house = House.builder()
                .id(11L)
                .searchCard(card)
                .address("서울시 강남구")
                .visitDateTime(LocalDateTime.of(2026, 4, 1, 10, 0))
                .build();

        when(houseRepository.findByIdAndSearchCardUserId(11L, 1L)).thenReturn(Optional.of(house));
        when(geocodingService.geocodeAddress("서울시 관악구"))
                .thenReturn(new GeocodingResultResponse(37.48, 126.95));

        HouseUpdateRequest req = new HouseUpdateRequest();
        ReflectionTestUtils.setField(req, "address", "서울시 관악구");
        ReflectionTestUtils.setField(req, "visitDateTime", LocalDateTime.of(2026, 4, 3, 11, 0));

        houseService.updateHouse(11L, 1L, req);

        verify(geocodingService).geocodeAddress("서울시 관악구");
        verify(reviewProducerService).sendHouseUpdatedEvent(any());
    }

    @Test
    void updateHousePropagatesGeocodingFailureWhenAddressChanged() {
        UUID cardId = UUID.randomUUID();
        SearchCard card = new SearchCard(1L, "카드", null, null);
        ReflectionTestUtils.setField(card, "id", cardId);
        House house = House.builder()
                .id(12L)
                .searchCard(card)
                .address("서울시 강남구")
                .visitDateTime(LocalDateTime.of(2026, 4, 1, 10, 0))
                .build();

        when(houseRepository.findByIdAndSearchCardUserId(12L, 1L)).thenReturn(Optional.of(house));
        when(geocodingService.geocodeAddress("서울시 관악구"))
                .thenThrow(new RuntimeException("geocode failed"));

        HouseUpdateRequest req = new HouseUpdateRequest();
        ReflectionTestUtils.setField(req, "address", "서울시 관악구");
        ReflectionTestUtils.setField(req, "visitDateTime", LocalDateTime.of(2026, 4, 3, 11, 0));

        assertThatThrownBy(() -> houseService.updateHouse(12L, 1L, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("geocode failed");
    }
}
