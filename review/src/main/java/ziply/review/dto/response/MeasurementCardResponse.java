package ziply.review.dto.response;

public record MeasurementCardResponse(Integer round, String title, boolean isDirectionDone, boolean isLightDone, String directionStatus,
                                      String lightStatus,
                                      Double direction,
                                      Double lightLevel
) {
}