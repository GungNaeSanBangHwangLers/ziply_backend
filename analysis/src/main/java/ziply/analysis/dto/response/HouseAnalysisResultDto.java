package ziply.analysis.dto.response;

import ziply.analysis.domain.HouseRouteAnalysis;

public record HouseAnalysisResultDto(
        Long houseId,
        Integer walkingTimeMin,
        Double walkingDistanceKm
) {
    public static HouseAnalysisResultDto from(HouseRouteAnalysis entity) {
        return new HouseAnalysisResultDto(
                entity.getHouseId(),
                entity.getWalkingTimeMin(),
                entity.getWalkingDistanceKm()
        );
    }
}
