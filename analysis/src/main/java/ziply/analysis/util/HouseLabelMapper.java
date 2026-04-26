package ziply.analysis.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import ziply.analysis.domain.HouseAnalysis;

@Component
public class HouseLabelMapper {

    public Map<Long, String> build(List<HouseAnalysis> allData) {
        List<Long> sortedIds = allData.stream()
                .map(HouseAnalysis::getHouseId)
                .distinct().sorted().toList();
        Map<Long, String> map = new HashMap<>();
        for (int i = 0; i < sortedIds.size(); i++) {
            map.put(sortedIds.get(i), String.valueOf((char) ('A' + i)));
        }
        return map;
    }
}
