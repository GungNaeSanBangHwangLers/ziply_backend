package ziply.analysis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ziply.analysis.domain.RoadSegmentEntity; // T3

public interface RoadSegmentRepository extends JpaRepository<RoadSegmentEntity, String> {
}