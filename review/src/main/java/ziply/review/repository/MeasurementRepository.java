package ziply.review.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ziply.review.domain.Measurement;
import java.util.List;

@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    List<Measurement> findAllByHouseIdOrderByRoundAsc(Long houseId);

    boolean existsByHouseIdAndRound(Long houseId, Integer round);

    Optional<Measurement> findByHouseIdAndRound(Long houseId, Integer round);
}