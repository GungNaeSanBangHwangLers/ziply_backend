package ziply.analysis.dto.response;

import ziply.analysis.domain.HouseAnalysis;

public record HouseAnalysisDto(
        Long houseId,
        Integer walkingTimeMin,
        Double walkingDistanceKm
) {
    public static HouseAnalysisDto from(HouseAnalysis entity) {
        return new HouseAnalysisDto(
                entity.getHouseId(),
                entity.getWalkingTimeMin(),
                entity.getWalkingDistanceKm()
        );
    }
}
