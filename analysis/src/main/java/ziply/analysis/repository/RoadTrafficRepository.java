package ziply.analysis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ziply.analysis.domain.RoadTrafficEntity; // T4

public interface RoadTrafficRepository extends JpaRepository<RoadTrafficEntity, Long> {
}