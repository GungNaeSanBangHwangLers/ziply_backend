package ziply.analysis.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchCardScoreAnalysis {
    Long houseId;
    Integer dayScore;
    Integer nightScore;
    Double avgScore;
    String message;
}