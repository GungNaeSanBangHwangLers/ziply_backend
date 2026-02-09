package ziply.analysis.dto.response;

import java.util.List;

public record BasePointAnalysisDto(
        List<HouseAnalysisDto> results,
        String transportMessage
) {}
