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
import ziply.review.dto.request.SearchCardCreateRequest;
import ziply.review.dto.response.BasePointAddressResponse;
import ziply.review.dto.response.GeocodingResultResponse;
import ziply.review.dto.response.MapInfoResponse;
import ziply.review.dto.response.SearchCardResponse;
import ziply.review.event.HouseCreatedEvent;
import ziply.review.event.HouseCreatedEvent.BasePointDetail;
import ziply.review.repository.BasePointRepository;
import ziply.review.repository.HouseRepository;
import ziply.review.repository.SearchCardRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SearchCardService {

    private final SearchCardRepository searchCardRepository;
    private final GeocodingService geocodingService;
    private final HouseRepository houseRepository;
    private final ReviewProducerService reviewProducerService;
    private final BasePointRepository basePointRepository;

    @Transactional
    public UUID createSearchCard(Long userId, SearchCardCreateRequest request) {
        var houseRequests = request.getHouses() != null ? request.getHouses() : new ArrayList<SearchCardCreateRequest.HouseCreateRequest>();

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

        // 2. 탐색 카드 기간(시작일/종료일) 계산
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

        // 3. 탐색 카드 객체 생성 (이전 주소/장단점 파라미터 제거됨)
        SearchCard searchCard = new SearchCard(
                userId,
                "새로운 탐색 카드",
                calculatedStart,
                calculatedEnd
        );

        // 4. 기점 정보(BasePoint)가 있을 경우 추가 및 지오코딩
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
                log.error("[INTEGRATED] 기점 지오코딩 실패: {}", request.getBasePointAddress());
            }
        }

        // 5. 탐색 카드 및 관련 집 정보 저장
        SearchCard savedCard = searchCardRepository.save(searchCard);

        for (House house : pendingHouses) {
            house.setSearchCard(savedCard);
        }
        List<House> savedHouses = houseRepository.saveAll(pendingHouses);

        // 6. 집 생성 이벤트 발행 (Kafka/Message Queue 등)
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

    @Transactional(readOnly = true)
    public List<MapInfoResponse> getMapInfo(UUID searchCardId, Long userId) {
        BasePoint basePoint = basePointRepository.findBySearchCardId(searchCardId)
                .orElseThrow(() -> new IllegalArgumentException("기점 정보를 찾을 수 없습니다."));

        if (!basePoint.getSearchCard().getUserId().equals(userId)) {
            throw new RuntimeException("해당 정보에 대한 접근 권한이 없습니다.");
        }

        List<House> houses = houseRepository.findBySearchCardIdOrderByIdAsc(searchCardId);

        List<MapInfoResponse> result = new ArrayList<>();

        result.add(MapInfoResponse.builder()
                .id(null)
                .label("기준지")
                .latitude(basePoint.getLatitude())
                .longitude(basePoint.getLongitude())
                .address(basePoint.getAddress())
                .build());

        for (int i = 0; i < houses.size(); i++) {
            House house = houses.get(i);
            String label = String.valueOf((char) ('A' + i));

            result.add(MapInfoResponse.builder()
                    .id(house.getId())
                    .label(label)
                    .latitude(house.getLatitude())
                    .longitude(house.getLongitude())
                    .address(house.getAddress())
                    .build());
        }

        return result;
    }
}