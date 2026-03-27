package ziply.analysis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ziply.analysis.domain.PoliceStation;

@Repository
public interface PoliceStationRepository extends JpaRepository<PoliceStation, Long> {
    @Query(value = "SELECT COUNT(*) FROM police_stations " +
            "WHERE ST_Distance_Sphere(point(longitude, latitude), point(:lon, :lat)) <= :distance",
            nativeQuery = true)
    int countNearby(@Param("lat") double lat, @Param("lon") double lon, @Param("distance") double distance);
}