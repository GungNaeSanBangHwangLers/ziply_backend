package ziply.analysis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ziply.analysis.domain.StreetLight;

@Repository
public interface StreetLightRepository extends JpaRepository<StreetLight, Long> {

    @Query(value = "SELECT COUNT(*) FROM street_lights " +
            "WHERE (latitude BETWEEN :minLat AND :maxLat) " +
            "AND (longitude BETWEEN :minLng AND :maxLng) " +
            "AND latitude > 0 AND longitude < 180",
            nativeQuery = true)
    int countInBoundingBox(@Param("minLat") double minLat,
                           @Param("maxLat") double maxLat,
                           @Param("minLng") double minLng,
                           @Param("maxLng") double maxLng);
}