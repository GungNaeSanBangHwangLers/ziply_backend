package ziply.review.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.review.domain.House;
import ziply.review.domain.SearchCard;
import ziply.review.dto.request.HouseCreateRequest;
import ziply.review.event.HouseCreatedEvent;
import ziply.review.event.HouseCreatedEvent.BasePointDetail;
import ziply.review.repository.HouseRepository;
import ziply.review.repository.SearchCardRepository;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class HouseService {

    private final HouseRepository houseRepository;
    private final SearchCardRepository searchCardRepository;
    private final ReviewProducerService producerService;
    private final GeocodingService geocodingService;

    public List<Long> createHouses(UUID cardId, List<HouseCreateRequest> requests, Long currentUserId) {
        log.info("[HOUSE] Starting creation of {} houses for SearchCard ID: {}", requests.size(), cardId);

        SearchCard searchCard = searchCardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("해당 탐색 카드를 찾을 수 없습니다."));

        if (!searchCard.getUserId().equals(currentUserId)) {
            throw new SecurityException("본인의 탐색 카드에만 집을 추가할 수 있습니다.");
        }

        requests.forEach(request -> {
            try {
                geocodingService.geocodeAddress(request);
            } catch (RuntimeException e) {
                log.error("[HOUSE] Geocoding 실패: 주소 {} 처리 중 오류 발생. 해당 집을 건너뜁니다.", request.getAddress(), e);
            }
        });
        List<House> houseList = requests.stream()
                .filter(request -> request.getLatitude() != null && request.getLongitude() != null)
                .map(request -> House.builder().searchCard(searchCard).address(request.getAddress())
                        .visitDateTime(request.getVisitDateTime()).latitude(request.getLatitude())
                        .longitude(request.getLongitude()).build()).collect(Collectors.toList());

        List<House> savedHouses = houseRepository.saveAll(houseList);
        log.info("[HOUSE] Successfully saved {} houses to DB. {} were skipped due to Geocoding failure.",
                savedHouses.size(), requests.size() - savedHouses.size());

        List<BasePointDetail> basePointDetails = searchCard.getBasePoints().stream()
                .map(bp -> BasePointDetail.builder().id(bp.getId()).latitude(bp.getLatitude())
                        .longitude(bp.getLongitude()).build()).collect(Collectors.toList());

        savedHouses.forEach(house -> {
            HouseCreatedEvent event = HouseCreatedEvent.builder().houseId(house.getId()).latitude(house.getLatitude())
                    .longitude(house.getLongitude()).timestamp(System.currentTimeMillis()).action("CREATED")
                    .searchCardId(searchCard.getId()).basePoints(basePointDetails).build();

            producerService.sendHouseCreatedEvent(event);
        });

        return savedHouses.stream().map(House::getId).toList();
    }
}