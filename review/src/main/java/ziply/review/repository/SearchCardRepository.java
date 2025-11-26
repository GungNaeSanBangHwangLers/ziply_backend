package ziply.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ziply.review.domain.SearchCard;

import java.util.List;

public interface SearchCardRepository extends JpaRepository<SearchCard, Long> {
    List<SearchCard> findAllByUserId(Long userId);
}