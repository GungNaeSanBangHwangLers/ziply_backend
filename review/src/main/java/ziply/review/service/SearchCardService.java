package ziply.review.service;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import ziply.review.dto.request.HouseCreateRequest;
import ziply.review.dto.request.SearchCardCreateRequest;
import ziply.review.dto.response.BasePointAddressResponse;
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
        // 타입 에러 방지를 위해 변수 타입을 명확히 하거나 var 사용
        var houseRequests = request.getHouses() != null ? request.getHouses() : new ArrayList<SearchCardCreateRequest.HouseCreateRequest>();

        // 1. 집 엔티티 리스트 준비 (지오코딩 포함)
        List<House> pendingHouses = houseRequests.stream()
                .map(hReq -> {
                    try {
                        GeocodingResultResponse geo = geocodingService.geocodeAddress(hReq.getAddress());
                        return House.builder()
                                .address(hReq.getAddress())
                                .visitDateTime(hReq.getVisitDateTime())
                                .latitude(geo.getLatitude())
                                .longitude(geo.getLongitude())
                                .build();
                    } catch (Exception e) {
                        log.warn("[INTEGRATED] 집 지오코딩 실패: {}", hReq.getAddress());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        // 2. 날짜 계산
        LocalDate calculatedStart = LocalDate.now();
        LocalDate calculatedEnd = null;

        if (!pendingHouses.isEmpty()) {
            List<LocalDateTime> dateTimes = pendingHouses.stream()
                    .map(House::getVisitDateTime)
                    .filter(Objects::nonNull)
                    .toList();

            if (!dateTimes.isEmpty()) {
                calculatedStart = dateTimes.stream().min(LocalDateTime::compareTo).get().toLocalDate();
                calculatedEnd = dateTimes.stream().max(LocalDateTime::compareTo).get().toLocalDate();
            }
        }

        // 3. 카드 생성 및 저장
        SearchCard searchCard = new SearchCard(userId, "새로운 탐색 카드", calculatedStart, calculatedEnd);

        // 기점 주소 처리
        if (request.getBasePointAddress() != null && !request.getBasePointAddress().isBlank()) {
            try {
                GeocodingResultResponse geoResult = geocodingService.geocodeAddress(request.getBasePointAddress());
                searchCard.addBasePoint(new BasePoint("기점", request.getBasePointAddress(), geoResult.getLatitude(), geoResult.getLongitude()));
            } catch (Exception e) {
                log.error("[INTEGRATED] 기점 지오코딩 실패");
            }
        }

        SearchCard savedCard = searchCardRepository.save(searchCard);

        // 4. 연관관계 매핑 및 집 저장
        for (House house : pendingHouses) {
            house.setSearchCard(savedCard);
        }
        List<House> savedHouses = houseRepository.saveAll(pendingHouses);

        // 5. 이벤트 발행
        if (!savedHouses.isEmpty()) {
            sendHouseCreatedEvents(savedCard, savedHouses);
        }

        return savedCard.getId();
    }

    @Transactional(readOnly = true)
    public List<BasePointAddressResponse> getBasePointAddresses(UUID searchCardId, Long userId) {
        SearchCard searchCard = searchCardRepository.findById(searchCardId)
                .orElseThrow(() -> new EntityNotFoundException("카드를 찾을 수 없습니다."));

        if (!searchCard.getUserId().equals(userId)) {
            throw new RuntimeException("해당 카드에 대한 접근 권한이 없습니다.");
        }

        return searchCard.getBasePoints().stream()
                .map(BasePointAddressResponse::from)
                .toList();
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