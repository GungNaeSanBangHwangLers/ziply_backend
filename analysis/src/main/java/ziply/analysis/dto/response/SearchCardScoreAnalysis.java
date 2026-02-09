package ziply.analysis.dto.response;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class SearchCardScoreAnalysis {
    Long houseId;
    String label;
    Integer dayScore;
    Integer nightScore;
    String message;
}