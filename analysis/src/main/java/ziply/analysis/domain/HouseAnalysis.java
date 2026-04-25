package ziply.analysis.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "house_analysis")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HouseAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID searchCardId;

    @Column(nullable = false)
    private Long houseId;

    @Column(nullable = false)
    private Long basePointId;

    private String basePointName;

    private Integer walkingTimeMin;
    private Double walkingDistanceKm;

    private Integer transitTimeMin;
    private String transitPaymentStr;
    private Integer transitDepth;

    private Integer carTimeMin;

    private Integer bikeTimeMin;

    private Integer dayScore;
    private Integer nightScore;

    private Integer safetyScore;
    private Integer policeCount;
    private Integer streetlightCount;
    private Integer cctvCount;

    @Column(name = "region_name")
    private String regionName;   // 법정동 이름 (예: 상도동) — 치안뉴스 조회에 활용
}