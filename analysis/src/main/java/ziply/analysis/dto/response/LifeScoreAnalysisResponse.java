package ziply.analysis.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LifeScoreAnalysisResponse {
    private Long houseId;
    private String label;
    private Integer dayScore;
    private Integer nightScore;
    private Integer schoolCount;
    private Integer subwayCount;
    private String message;
}