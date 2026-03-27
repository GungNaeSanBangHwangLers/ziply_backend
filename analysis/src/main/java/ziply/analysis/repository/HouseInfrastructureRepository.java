package ziply.analysis.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ziply.analysis.domain.HouseInfrastructure;

public interface HouseInfrastructureRepository extends JpaRepository<HouseInfrastructure, Long> {
    boolean existsByHouseId(Long houseId);
    void deleteByHouseId(Long houseId);

    Optional<HouseInfrastructure> findByHouseId(Long houseId);
}