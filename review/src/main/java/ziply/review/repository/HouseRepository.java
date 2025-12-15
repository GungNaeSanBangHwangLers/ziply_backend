package ziply.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ziply.review.domain.House;

import java.util.List;
import java.util.UUID;

public interface HouseRepository extends JpaRepository<House, Long> {
    List<House> findBySearchCardId(UUID searchCardId);
}