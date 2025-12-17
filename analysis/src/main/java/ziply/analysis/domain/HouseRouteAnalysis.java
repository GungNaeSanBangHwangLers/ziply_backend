package ziply.analysis.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "house_route_analysis")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HouseRouteAnalysis {

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
}