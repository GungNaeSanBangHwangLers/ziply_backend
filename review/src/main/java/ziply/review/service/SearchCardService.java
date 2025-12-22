package ziply.review.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.review.domain.BasePoint;
import ziply.review.domain.SearchCard;
import ziply.review.dto.request.SearchCardCreateRequest;
import ziply.review.dto.response.GeocodingResultResponse;
import ziply.review.dto.response.SearchCardResponse;
import ziply.review.repository.SearchCardRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SearchCardService {

    private final SearchCardRepository searchCardRepository;
    private final GeocodingService geocodingService;

    @Transactional
    public UUID createSearchCard(Long userId, SearchCardCreateRequest request) {
        log.info("[REVIEW] Creating search card for user: {}, title: {}", userId, request.getTitle());

        SearchCard searchCard = new SearchCard(userId, request.getTitle(), request.getStartDate(),
                request.getEndDate());

        if (request.getBasePoints() != null && !request.getBasePoints().isEmpty()) {
            log.debug("[REVIEW] Adding {} base points to search card (Geocoding started)",
                    request.getBasePoints().size());

            for (SearchCardCreateRequest.BasePointRequest bpDto : request.getBasePoints()) {
                String address = bpDto.getAddress();

                if (address == null || address.isEmpty()) {
                    log.warn("[REVIEW] Skipping base point with empty address: {}", bpDto.getAlias());
                    continue;
                }

                GeocodingResultResponse geoResult;
                try {
                    geoResult = geocodingService.geocodeAddress(address);
                } catch (RuntimeException e) {
                    log.warn("[REVIEW] Skipping base point due to geocoding failure for address: {}. Error: {}",
                            address, e.getMessage());
                    continue;
                }
                BasePoint basePoint = new BasePoint(bpDto.getAlias(), bpDto.getAddress(), geoResult.getLatitude(),
                        geoResult.getLongitude());
                searchCard.addBasePoint(basePoint);
            }
        }

        SearchCard saved = searchCardRepository.save(searchCard);
        log.info("[REVIEW] Search card created successfully, id: {}, userId: {}", saved.getId(), userId);
        return saved.getId();
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