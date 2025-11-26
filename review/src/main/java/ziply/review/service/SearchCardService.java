package ziply.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.review.domain.BasePoint;
import ziply.review.domain.SearchCard;
import ziply.review.dto.request.SearchCardCreateRequest;
import ziply.review.repository.SearchCardRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchCardService {

    private final SearchCardRepository searchCardRepository;

    @Transactional
    public Long createSearchCard(Long userId, SearchCardCreateRequest request) {
        SearchCard searchCard = new SearchCard(
                userId,
                request.getTitle(),
                request.getStartDate(),
                request.getEndDate()
        );

        if (request.getBasePoints() != null) {
            for (SearchCardCreateRequest.BasePointRequest bpDto : request.getBasePoints()) {
                BasePoint basePoint = new BasePoint(
                        bpDto.getAlias(),
                        bpDto.getAddress(),
                        bpDto.getLatitude(),
                        bpDto.getLongitude()
                );
                searchCard.addBasePoint(basePoint);
            }
        }

        return searchCardRepository.save(searchCard).getId();
    }
}