package ziply.review.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ziply.review.domain.House;

import java.util.List;
import java.util.UUID;
import ziply.review.domain.SearchCard;

public interface HouseRepository extends JpaRepository<House, Long> {
    List<House> findBySearchCardId(UUID searchCardId);
    Optional<House> findByIdAndSearchCardUserId(Long id, Long userId);

    long countBySearchCard(SearchCard card);
}