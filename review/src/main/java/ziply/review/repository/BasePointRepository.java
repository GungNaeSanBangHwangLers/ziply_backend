package ziply.review.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ziply.review.domain.BasePoint;

public interface BasePointRepository extends JpaRepository<BasePoint, Long> {
    Optional<BasePoint> findBySearchCardId(UUID searchCardId);
}