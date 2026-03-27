package ziply.review.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ziply.review.domain.House;

import java.util.List;
import java.util.UUID;
import ziply.review.domain.SearchCard;
import ziply.review.dto.response.AddressInfo;
import ziply.review.dto.response.UserAddressResponse;

public interface HouseRepository extends JpaRepository<House, Long> {
    List<House> findBySearchCardId(UUID searchCardId);

    Optional<House> findByIdAndSearchCardUserId(Long id, Long userId);

    long countBySearchCard(SearchCard card);

    List<House> findBySearchCardIdOrderByIdAsc(UUID searchCardId);

    @Query("SELECT DISTINCT new ziply.review.dto.response.AddressInfo(h.address, h.latitude, h.longitude) " +
            "FROM House h " +
            "JOIN h.searchCard sc " +
            "WHERE sc.userId = :userId AND h.address IS NOT NULL")
    List<AddressInfo> findDistinctAddressInfosByUserId(@Param("userId") Long userId);
}