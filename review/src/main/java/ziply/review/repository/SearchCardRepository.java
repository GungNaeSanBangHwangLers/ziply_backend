package ziply.review.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ziply.review.domain.SearchCard;

import java.util.List;

public interface SearchCardRepository extends JpaRepository<SearchCard, UUID> {
    List<SearchCard> findAllByUserId(Long userId);
    Optional<SearchCard> findByIdAndUserId(UUID id, Long userId);
}