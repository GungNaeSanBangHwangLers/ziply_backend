package ziply.analysis.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyAnalysisResponse {
    private Long houseId;
    private String label;
    private Integer safetyScore;
    private Integer policeCount;
    private Integer streetlightCount;
    private Integer cctvCount;
    private String message;
}