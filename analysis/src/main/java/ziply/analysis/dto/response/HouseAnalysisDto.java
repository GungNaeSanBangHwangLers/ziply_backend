package ziply.analysis.dto.response;

import ziply.analysis.domain.HouseAnalysis;

public record HouseAnalysisDto(
        Long houseId,
        String label,
        Integer walkingTimeMin,
        Double walkingDistanceKm,
        Integer transitTimeMin,
        String transitPaymentStr,
        Integer transitDepth,
        Integer carTimeMin
) {
    public static HouseAnalysisDto from(HouseAnalysis entity, String label) {
        return new HouseAnalysisDto(
                entity.getHouseId(),
                label,
                entity.getWalkingTimeMin(),
                entity.getWalkingDistanceKm(),
                entity.getTransitTimeMin(),
                entity.getTransitPaymentStr(),
                entity.getTransitDepth(),
                entity.getCarTimeMin()
        );
    }
}