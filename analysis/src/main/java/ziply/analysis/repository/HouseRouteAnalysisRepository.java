package ziply.analysis.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ziply.analysis.domain.HouseAnalysis;

public interface HouseRouteAnalysisRepository extends JpaRepository<HouseAnalysis, Long> {
    List<HouseAnalysis> findBySearchCardId(UUID searchCardId);

    void deleteByHouseId(Long houseId);

    void deleteBySearchCardId(UUID searchCardId);

    @Query(value = "SELECT COUNT(*) FROM ziply_review.search_cards WHERE id = :searchCardId AND user_id = :userId", nativeQuery = true)
    Long countBySearchCardIdAndUserId(@Param("searchCardId") UUID searchCardId, @Param("userId") Long userId);
}