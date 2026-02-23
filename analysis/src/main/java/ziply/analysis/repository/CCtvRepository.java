package ziply.analysis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ziply.analysis.domain.CCtv;

@Repository
public interface CCtvRepository extends JpaRepository<CCtv, Long> {
    @Query(value = "SELECT SUM(quantity) FROM cctv " +
            "WHERE ST_Distance_Sphere(point(longitude, latitude), point(:lon, :lat)) <= :distance",
            nativeQuery = true)
    Integer sumQtyNearby(@Param("lat") double lat, @Param("lon") double lon, @Param("distance") double distance);
}