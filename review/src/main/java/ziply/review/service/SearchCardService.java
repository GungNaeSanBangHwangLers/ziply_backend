package ziply.review.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.review.domain.BasePoint;
import ziply.review.domain.House;
import ziply.review.domain.SearchCard;
import ziply.review.dto.request.SearchCardCreateRequest;
import ziply.review.dto.response.GeocodingResultResponse;
import ziply.review.dto.response.SearchCardResponse;
import ziply.review.event.HouseCreatedEvent;
import ziply.review.event.HouseCreatedEvent.BasePointDetail;
import ziply.review.repository.HouseRepository;
import ziply.review.repository.SearchCardRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SearchCardService {

    private final SearchCardRepository searchCardRepository;
    private final GeocodingService geocodingService;
    private  final HouseRepository houseRepository;
    private final ReviewProducerService  reviewProducerService;

    @Transactional
    public UUID createSearchCard(Long userId, SearchCardCreateRequest request) {
        log.info("[INTEGRATED] 통합 카드 생성 시작 - User: {}, BaseAddress: {}", userId, request.getBasePointAddress());

        SearchCard searchCard = new SearchCard(
                userId,
                "새로운 탐색 카드",
                LocalDate.now(),
                null
        );

        if (request.getBasePointAddress() != null && !request.getBasePointAddress().isBlank()) {
            try {
                GeocodingResultResponse geoResult = geocodingService.geocodeAddress(request.getBasePointAddress());
                searchCard.addBasePoint(new BasePoint(
                        "기점",
                        request.getBasePointAddress(),
                        geoResult.getLatitude(),
                        geoResult.getLongitude()
                ));
            } catch (Exception e) {
                log.error("[INTEGRATED] 기점 지오코딩 실패: {} - 사유: {}", request.getBasePointAddress(), e.getMessage());
            }
        }

        SearchCard savedCard = searchCardRepository.save(searchCard);

        List<House> savedHouses = new ArrayList<>();
        if (request.getHouses() != null && !request.getHouses().isEmpty()) {
            List<House> houseList = request.getHouses().stream()
                    .map(hReq -> {
                        try {
                            log.info("[INTEGRATED] 집 지오코딩 시도: {}", hReq.getAddress());
                            GeocodingResultResponse geo = geocodingService.geocodeAddress(hReq.getAddress());

                            hReq.setLatitude(geo.getLatitude());
                            hReq.setLongitude(geo.getLongitude());

                            return House.builder()
                                    .searchCard(savedCard)
                                    .address(hReq.getAddress())
                                    .visitDateTime(hReq.getVisitDateTime())
                                    .latitude(geo.getLatitude())
                                    .longitude(geo.getLongitude())
                                    .build();
                        } catch (Exception e) {
                            log.warn("[INTEGRATED] 집 지오코딩 실패 (건너뜀): {} - 사유: {}", hReq.getAddress(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            if (!houseList.isEmpty()) {
                savedHouses = houseRepository.saveAll(houseList);
                log.info("[INTEGRATED] {}개의 집 정보 저장 완료", savedHouses.size());
            }
        }

        if (!savedHouses.isEmpty()) {
            sendHouseCreatedEvents(savedCard, savedHouses);
        }

        log.info("[INTEGRATED] 모든 프로세스 완료. CardID: {}", savedCard.getId());
        return savedCard.getId();
    }

    private void sendHouseCreatedEvents(SearchCard card, List<House> houses) {
        List<BasePointDetail> basePointDetails = card.getBasePoints().stream()
                .map(bp -> BasePointDetail.builder()
                        .id(bp.getId())
                        .name(bp.getAlias())
                        .latitude(bp.getLatitude())
                        .longitude(bp.getLongitude())
                        .build())
                .toList();

        houses.forEach(house -> {
            HouseCreatedEvent event = HouseCreatedEvent.builder()
                    .houseId(house.getId())
                    .latitude(house.getLatitude())
                    .longitude(house.getLongitude())
                    .searchCardId(card.getId())
                    .basePoints(basePointDetails)
                    .action("CREATED")
                    .timestamp(System.currentTimeMillis())
                    .build();

            reviewProducerService.sendHouseCreatedEvent(event);
        });
    }

    @Transactional(readOnly = true)
    public List<SearchCardResponse> getSearchCards(Long userId) {
        List<SearchCard> cards = searchCardRepository.findAllByUserId(userId);

        return cards.stream().map(SearchCardResponse::from).collect(Collectors.toList());
    }

    public boolean isCardOwner(UUID searchCardId, Long userId) {
        return searchCardRepository.existsByIdAndUserId(searchCardId, userId);
    }
}