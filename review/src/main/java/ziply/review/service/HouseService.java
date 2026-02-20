package ziply.review.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.review.domain.House;
import ziply.review.domain.Measurement;
import ziply.review.domain.SearchCard;
import ziply.review.dto.request.HouseCreateRequest;
import ziply.review.dto.request.HouseUpdateRequest;
import ziply.review.dto.response.GeocodingResultResponse;
import ziply.review.dto.response.HouseListResponse;
import ziply.review.dto.response.AddressInfo;
import ziply.review.event.HouseCreatedEvent;
import ziply.review.event.HouseCreatedEvent.BasePointDetail;
import ziply.review.event.HouseUpdatedEvent;
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
    private final ReviewProducerService reviewProducerService;

    public List<Long> createHouses(UUID cardId, List<HouseCreateRequest> requests, Long currentUserId) {
        log.info("[HOUSE] Starting creation of {} houses for SearchCard ID: {}", requests.size(), cardId);

        SearchCard searchCard = searchCardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("해당 탐색 카드를 찾을 수 없습니다."));

        if (!searchCard.getUserId().equals(currentUserId)) {
            throw new SecurityException("본인의 탐색 카드에만 집을 추가할 수 있습니다.");
        }

        long existingHouseCount = houseRepository.countBySearchCard(searchCard);
        if (existingHouseCount + requests.size() > 7) {
            throw new IllegalArgumentException("한 카드당 집은 최대 7개까지 등록할 수 있습니다. (현재: " + existingHouseCount + "개)");
        }

        requests.forEach(request -> {
            try {
                geocodingService.geocodeAddress(request);
            } catch (RuntimeException e) {
                log.error("[HOUSE] Geocoding 실패: 주소 {} 처리 중 오류 발생. 해당 집을 건너뜁니다.", request.getAddress());
            }
        });

        List<House> houseList = requests.stream()
                .filter(request -> request.getLatitude() != null && request.getLongitude() != null)
                .map(request -> House.builder()
                        .searchCard(searchCard)
                        .address(request.getAddress())
                        .visitDateTime(request.getVisitDateTime())
                        .latitude(request.getLatitude())
                        .longitude(request.getLongitude())
                        .build())
                .collect(Collectors.toList());

        List<House> savedHouses = houseRepository.saveAll(houseList);
        log.info("[HOUSE] Successfully saved {} houses to DB. {} were skipped due to Geocoding failure.",
                savedHouses.size(), requests.size() - savedHouses.size());

        List<BasePointDetail> basePointDetails = searchCard.getBasePoints().stream()
                .map(bp -> BasePointDetail.builder()
                        .id(bp.getId())
                        .name(bp.getAlias())
                        .latitude(bp.getLatitude())
                        .longitude(bp.getLongitude())
                        .build())
                .collect(Collectors.toList());

        savedHouses.forEach(house -> {
            HouseCreatedEvent event = HouseCreatedEvent.builder()
                    .houseId(house.getId())
                    .latitude(house.getLatitude())
                    .longitude(house.getLongitude())
                    .timestamp(System.currentTimeMillis())
                    .action("CREATED")
                    .searchCardId(searchCard.getId())
                    .basePoints(basePointDetails)
                    .build();

            producerService.sendHouseCreatedEvent(event);
        });
        return savedHouses.stream().map(House::getId).toList();
    }

    public List<AddressInfo> getAllUniqueAddresses(Long userId) {
        return houseRepository.findDistinctAddressInfosByUserId(userId);
    }

    public List<HouseListResponse> getHousesBySearchCard(UUID searchCardId, Long userId) {
        searchCardRepository.findByIdAndUserId(searchCardId, userId)
                .orElseThrow(() -> new IllegalArgumentException("접근 권한이 없거나 존재하지 않는 카드입니다."));

        List<House> houses = houseRepository.findBySearchCardId(searchCardId);

        return IntStream.range(0, houses.size())
                .mapToObj(i -> {
                    House house = houses.get(i);
                    String label = String.valueOf((char) ('A' + i));

                    return HouseListResponse.builder()
                            .houseId(house.getId())
                            .address(house.getAddress())
                            .visitTime(house.getVisitDateTime())
                            .label(label)
                            .imageUrls(house.getMeasurements().stream()
                                    .filter(m -> m.getImageUrl() != null && !m.getImageUrl().isBlank())
                                    .sorted(Comparator.comparing(Measurement::getRound,
                                            Comparator.nullsLast(Comparator.naturalOrder())))
                                    .map(Measurement::getImageUrl)
                                    .collect(Collectors.toList()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateHouse(Long houseId, Long userId, HouseUpdateRequest request) {
        House house = houseRepository.findByIdAndSearchCardUserId(houseId, userId)
                .orElseThrow(() -> new IllegalArgumentException("수정 권한이 없거나 존재하지 않는 집입니다."));

        String oldAddress = house.getAddress();
        String newAddress = request.getAddress();

        house.update(newAddress, request.getVisitDateTime());

        if (!oldAddress.equals(newAddress)) {

            GeocodingResultResponse geocodingResult = geocodingService.geocodeAddress(newAddress);

            List<HouseUpdatedEvent.BasePointDetail> basePoints = house.getSearchCard().getBasePoints().stream()
                    .map(bp -> HouseUpdatedEvent.BasePointDetail.builder()
                            .id(bp.getId())
                            .name(bp.getAlias())
                            .latitude(bp.getLatitude())
                            .longitude(bp.getLongitude())
                            .build())
                    .toList();

            HouseUpdatedEvent event = HouseUpdatedEvent.builder()
                    .houseId(house.getId())
                    .searchCardId(house.getSearchCard().getId())
                    .address(newAddress)
                    .latitude(geocodingResult.getLatitude())
                    .longitude(geocodingResult.getLongitude())
                    .basePoints(basePoints)
                    .timestamp(System.currentTimeMillis())
                    .build();

            reviewProducerService.sendHouseUpdatedEvent(event);
        }
    }

    @Transactional
    public void deleteHouse(Long houseId, Long userId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("집 없음"));

        SearchCard card = house.getSearchCard();

        if (!card.getUserId().equals(userId)) {
            throw new IllegalStateException("권한 없음");
        }

        houseRepository.delete(house);
        log.info("[Review] House 삭제 완료: {}", houseId);

        long remainingHouses = houseRepository.countBySearchCard(card);

        if (remainingHouses == 0) {
            log.info("[Review] 남은 집이 없어 주거탐색카드({})를 삭제합니다.", card.getId());
            searchCardRepository.delete(card);

            reviewProducerService.sendCardDeletedEvent(card.getId());
        }

        reviewProducerService.sendDeleteSignal(houseId);
    }
}