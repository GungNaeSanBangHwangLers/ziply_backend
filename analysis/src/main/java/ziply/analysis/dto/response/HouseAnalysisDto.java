package ziply.analysis.dto.response;

import lombok.Builder;
import lombok.Getter;
import ziply.analysis.domain.HouseAnalysis;

@Getter
@Builder
public class HouseAnalysisDto {
    private Long houseId;
    private String label;
    private Integer walkingTimeMin;
    private Integer bikeTimeMin;
    private Integer transitTimeMin;
    private Integer carTimeMin;
    private String transitPaymentStr;

    public static HouseAnalysisDto from(HouseAnalysis entity, String label) {
        return HouseAnalysisDto.builder()
                .houseId(entity.getHouseId())
                .label(label)
                .walkingTimeMin(entity.getWalkingTimeMin())
                .bikeTimeMin(entity.getBikeTimeMin())
                .transitTimeMin(entity.getTransitTimeMin())
                .carTimeMin(entity.getCarTimeMin())
                .transitPaymentStr(entity.getTransitPaymentStr())
                .build();
    }
}