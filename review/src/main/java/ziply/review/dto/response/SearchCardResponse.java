package ziply.review.dto.response;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import ziply.review.domain.SearchCard;
import ziply.review.domain.SearchCardStatus;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCardResponse {

    private UUID cardId;
    private String title;
    private Integer houseCount;
    private LocalDate startDate;
    private LocalDate endDate;
    private SearchCardStatus status;

    public static SearchCardResponse from(SearchCard searchCard) {
        return SearchCardResponse.builder()
                .cardId(searchCard.getId())
                .title(searchCard.getTitle())
                .houseCount(searchCard.getHouses() != null ? searchCard.getHouses().size() : 0)
                .startDate(searchCard.getStartDate())
                .endDate(searchCard.getEndDate())
                .status(searchCard.getStatus())
                .build();
    }
}