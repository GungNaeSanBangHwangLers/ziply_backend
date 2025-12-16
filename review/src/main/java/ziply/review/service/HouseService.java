package ziply.review.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.review.domain.House;
import ziply.review.domain.SearchCard;
import ziply.review.dto.request.HouseCreateRequest;
import ziply.review.event.HouseCreatedEvent;
import ziply.review.repository.HouseRepository;
import ziply.review.repository.SearchCardRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class HouseService {

    private final HouseRepository houseRepository;
    private final SearchCardRepository searchCardRepository;
    private final ReviewProducerService producerService;
    private final GeocodingService geocodingService;

    public List<Long> createHouses(UUID cardId, List<HouseCreateRequest> requests, Long currentUserId) {
        SearchCard searchCard = searchCardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("해당 탐색 카드를 찾을 수 없습니다."));

        if (!searchCard.getUserId().equals(currentUserId)) {
            throw new SecurityException("본인의 탐색 카드에만 집을 추가할 수 있습니다.");
        }

        requests.forEach(geocodingService::geocodeAddress);

        List<House> houseList = requests.stream()
                .map(request -> House.builder()
                        .searchCard(searchCard)
                        .address(request.getAddress())
                        .visitDateTime(request.getVisitDateTime())
                        .latitude(request.getLatitude())
                        .longitude(request.getLongitude())
                        .build())
                .toList();

        List<House> savedHouses = houseRepository.saveAll(houseList);

        savedHouses.forEach(house -> {
            HouseCreatedEvent event = HouseCreatedEvent.builder()
                    .houseId(house.getId())
                    .latitude(house.getLatitude())
                    .longitude(house.getLongitude())
                    .searchCardId(searchCard.getId())
                    .build();

            producerService.sendHouseCreatedEvent(event);
        });

        return savedHouses.stream()
                .map(House::getId)
                .toList();
    }
}