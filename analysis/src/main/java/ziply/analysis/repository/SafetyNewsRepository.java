package ziply.analysis.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ziply.analysis.domain.SafetyNews;

public interface SafetyNewsRepository extends JpaRepository<SafetyNews, Long> {

    /**
     * 동 이름과 기간으로 치안 뉴스 조회 (전체 목록, 레벨 집계용).
     */
    @Query("SELECT s FROM SafetyNews s " +
           "WHERE s.regionName LIKE %:regionName% " +
           "AND s.publishedAt >= :since " +
           "ORDER BY s.publishedAt DESC")
    List<SafetyNews> findByRegionAndPeriod(
            @Param("regionName") String regionName,
            @Param("since") LocalDateTime since);

    /**
     * 동 이름과 기간으로 치안 뉴스 페이지네이션 조회.
     */
    @Query("SELECT s FROM SafetyNews s " +
           "WHERE s.regionName LIKE %:regionName% " +
           "AND s.publishedAt >= :since")
    Page<SafetyNews> findByRegionAndPeriodPageable(
            @Param("regionName") String regionName,
            @Param("since") LocalDateTime since,
            Pageable pageable);

    /**
     * 레벨별 개수 집계용 (동 이름 + 기간)
     */
    @Query("SELECT s.categoryLevel, COUNT(s) FROM SafetyNews s " +
           "WHERE s.regionName LIKE %:regionName% " +
           "AND s.publishedAt >= :since " +
           "GROUP BY s.categoryLevel")
    List<Object[]> countByLevelAndRegion(
            @Param("regionName") String regionName,
            @Param("since") LocalDateTime since);
}
