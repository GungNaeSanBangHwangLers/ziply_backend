package ziply.analysis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ziply.analysis.domain.HouseRouteAnalysis;

public interface HouseRouteAnalysisRepository extends JpaRepository<HouseRouteAnalysis, Long> {
}