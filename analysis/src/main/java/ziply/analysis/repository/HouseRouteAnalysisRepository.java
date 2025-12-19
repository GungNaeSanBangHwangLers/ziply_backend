package ziply.analysis.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ziply.analysis.domain.HouseAnalysis;

public interface HouseRouteAnalysisRepository extends JpaRepository<HouseAnalysis, Long> {
    List<HouseAnalysis> findBySearchCardId(UUID searchCardId);
    void deleteByHouseId(Long houseId);
    void deleteBySearchCardId(UUID searchCardId);
}