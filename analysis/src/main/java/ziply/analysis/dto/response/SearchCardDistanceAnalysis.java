package ziply.analysis.dto.response;

import java.util.List;

public record SearchCardDistanceAnalysis(
        List<BasePointAnalysisDto> basePoints
) {}

