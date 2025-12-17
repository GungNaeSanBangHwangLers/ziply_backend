package ziply.analysis.dto.response;

import java.util.List;
import java.util.UUID;

public record SearchCardAnalysisResponse(
        UUID searchCardId,
        List<BasePointAnalysisDto> basePoints
) {}

