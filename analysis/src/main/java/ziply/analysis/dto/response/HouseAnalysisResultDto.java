package ziply.analysis.dto.response;

import ziply.analysis.domain.HouseAnalysis;

public record HouseAnalysisResultDto(
        Long houseId,
        Integer walkingTimeMin,
        Double walkingDistanceKm,
        Integer DayScore,
        Integer NightScore
) {
    public static HouseAnalysisResultDto from(HouseAnalysis entity) {
        return new HouseAnalysisResultDto(
                entity.getHouseId(),
                entity.getWalkingTimeMin(),
                entity.getWalkingDistanceKm(),
                entity.getDayScore(),
                entity.getNightScore()
        );
    }
}
