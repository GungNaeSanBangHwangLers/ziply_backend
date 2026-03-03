package ziply.review.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ziply.review.domain.HouseImage;

@Repository
public interface HouseImageRepository extends JpaRepository<HouseImage, Long> {

    // 특정 하우스에 속한 모든 이미지 조회
    List<HouseImage> findAllByHouseId(Long houseId);

    // 재측정 또는 삭제 시 해당 하우스의 모든 이미지 삭제
    void deleteAllByHouseId(Long houseId);
}