package ziply.review.service;

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
        log.info("[INTEGRATED] 통합 카드 생성 시작 - User: {}, Title: {}", userId, request.getTitle());

        // 1. SearchCard 기본 정보 생성
        SearchCard searchCard = new SearchCard(userId, request.getTitle(), request.getStartDate(), request.getEndDate());

        // 2. 기점(BasePoint) 지오코딩 및 추가
        if (request.getBasePoints() != null) {
            for (var bpDto : request.getBasePoints()) {
                try {
                    // String address를 인자로 받는 메서드 사용
                    GeocodingResultResponse geoResult = geocodingService.geocodeAddress(bpDto.getAddress());
                    searchCard.addBasePoint(new BasePoint(
                            bpDto.getAlias(),
                            bpDto.getAddress(),
                            geoResult.getLatitude(),
                            geoResult.getLongitude()
                    ));
                } catch (RuntimeException e) {
                    log.warn("[INTEGRATED] 기점 지오코딩 실패 (건너뜀): {}", bpDto.getAddress());
                }
            }
        }

        // 카드와 기점 정보를 먼저 DB에 저장
        SearchCard savedCard = searchCardRepository.save(searchCard);

        // 3. 집(House) 리스트 처리 및 저장
        List<House> savedHouses = new ArrayList<>();
        if (request.getHouses() != null && !request.getHouses().isEmpty()) {
            List<House> houseList = request.getHouses().stream()
                    .map(hReq -> {
                        try {
                            // 기존의 String 인자 메서드를 활용하여 좌표를 가져옴
                            GeocodingResultResponse geo = geocodingService.geocodeAddress(hReq.getAddress());

                            // DTO에 좌표 세팅 (Kafka 이벤트나 이후 로직을 위해 필요)
                            hReq.setLatitude(geo.getLatitude());
                            hReq.setLongitude(geo.getLongitude());

                            return House.builder()
                                    .searchCard(savedCard)
                                    .address(hReq.getAddress())
                                    .visitDateTime(hReq.getVisitDateTime())
                                    .latitude(geo.getLatitude())
                                    .longitude(geo.getLongitude())
                                    .build();
                        } catch (RuntimeException e) {
                            log.warn("[INTEGRATED] 집 지오코딩 실패 (건너뜀): {}", hReq.getAddress());
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

        // 4. Kafka 이벤트 발행
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