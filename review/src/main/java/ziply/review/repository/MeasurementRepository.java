package ziply.review.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ziply.review.domain.Measurement;

@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    List<Measurement> findAllByHouseId(Long houseId);

    @Query("SELECT m FROM Measurement m JOIN m.house h WHERE h.searchCard.id = :searchCardId")
    List<Measurement> findAllBySearchCardId(@Param("searchCardId") UUID searchCardId);

    @Modifying
    @Query("DELETE FROM Measurement m WHERE m.house.id = :houseId")
    void deleteAllByHouseId(@Param("houseId") Long houseId);

    Optional<Measurement> findByHouseIdAndRound(Long houseId, Integer round);

    @Modifying
    @Query("UPDATE Measurement m SET m.round = m.round - 1 " +
            "WHERE m.house.id = :houseId AND m.round > :deletedRound")
    void reorderRounds(@Param("houseId") Long houseId, @Param("deletedRound") Integer deletedRound);

    boolean existsByHouseId(Long houseId);

    int countByHouseId(Long houseId);
}