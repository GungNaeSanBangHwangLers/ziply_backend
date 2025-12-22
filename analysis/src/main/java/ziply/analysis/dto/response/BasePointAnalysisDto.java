package ziply.analysis.dto.response;

import java.util.List;

public record BasePointAnalysisDto(
        Long basePointId,
        String basePointName,
        List<HouseAnalysisDto> results
) {}
