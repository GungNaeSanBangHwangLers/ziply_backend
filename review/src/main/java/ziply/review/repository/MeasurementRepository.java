package ziply.review.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ziply.review.domain.Measurement;
import java.util.List;

@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    List<Measurement> findAllByHouseIdOrderByRoundAsc(Long houseId);

    boolean existsByHouseIdAndRound(Long houseId, Integer round);

    Optional<Measurement> findByHouseIdAndRound(Long houseId, Integer round);

    @Query("SELECT m FROM Measurement m JOIN m.house h WHERE h.searchCard.id = :searchCardId")
    List<Measurement> findAllBySearchCardId(@Param("searchCardId") UUID searchCardId);
}