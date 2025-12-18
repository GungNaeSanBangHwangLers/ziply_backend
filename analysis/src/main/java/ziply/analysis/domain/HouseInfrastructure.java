package ziply.analysis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "house_infrastructure_analysis")
public class HouseInfrastructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long houseId; // 집 식별자

    private int schoolCount;    // 주변 학교 수
    private int restaurantCount; // 주변 음식점 수
    private int subwayCount;     // 주변 지하철역 수

    @Builder
    public HouseInfrastructure(Long houseId, int schoolCount, int restaurantCount, int subwayCount) {
        this.houseId = houseId;
        this.schoolCount = schoolCount;
        this.restaurantCount = restaurantCount;
        this.subwayCount = subwayCount;
    }
}