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
    private Long houseId;

    private int schoolCount;
    private int restaurantCount;
    private int subwayCount;

    @Builder
    public HouseInfrastructure(Long houseId, int schoolCount, int restaurantCount, int subwayCount) {
        this.houseId = houseId;
        this.schoolCount = schoolCount;
        this.restaurantCount = restaurantCount;
        this.subwayCount = subwayCount;
    }
}