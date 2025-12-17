package ziply.analysis.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ziply.analysis.domain.HouseRouteAnalysis;

public interface HouseRouteAnalysisRepository extends JpaRepository<HouseRouteAnalysis, Long> {
    List<HouseRouteAnalysis> findBySearchCardId(UUID searchCardId);
    void deleteByHouseId(Long houseId);

    void deleteBySearchCardId(UUID searchCardId);
}