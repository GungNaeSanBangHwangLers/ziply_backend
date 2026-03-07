package ziply.review.domain;

import jakarta.persistence.*;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "measurements")
public class Measurement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id")
    private House house;

    private Integer round;
    private Double direction;
    private Double lightLevel;
    private String directionType;
    private String windowLocation;

    @Column(length = 1000)
    private String directionFeatures;

    @Column(length = 1000)
    private String directionPros;

    @Column(length = 1000)
    private String directionCons;

    @Builder
    public Measurement(House house, Integer round, Double direction, Double lightLevel,
                       String directionType, String windowLocation,
                       String directionFeatures, String directionPros, String directionCons) {
        this.house = house;
        this.round = round;
        this.direction = (direction != null) ? normalizeDirection(direction) : null;
        this.lightLevel = lightLevel;
        this.directionType = directionType;
        this.windowLocation = windowLocation;
        this.directionFeatures = directionFeatures;
        this.directionPros = directionPros;
        this.directionCons = directionCons;
    }

    /**
     * 방향 관련 정보 통합 업데이트
     * Service에서 넘어오는 List<String> 타입을 DB 저장을 위해 String으로 변환합니다.
     */
    public void updateDirection(Double direction, String directionType, String features,
                                String pros, String cons, String windowLocation) {
        if (direction != null) {
            this.direction = normalizeDirection(direction);
        }
        this.directionType = directionType;
        this.directionFeatures = features;
        this.directionPros = pros;
        this.directionCons = cons;
        this.windowLocation = windowLocation;
    }

    /**
     * 채광 정보 업데이트
     */
    public void updateLightLevel(Double lightLevel) {
        this.lightLevel = lightLevel;
    }

    /**
     * 하우스 연관관계 설정 (양방향 편의 메서드)
     */
    public void assignHouse(House house) {
        if (this.house != null) {
            this.house.getMeasurements().remove(this);
        }
        this.house = house;
        if (house != null && !house.getMeasurements().contains(this)) {
            house.getMeasurements().add(this);
        }
    }

    /**
     * 방위각 정규화 (0~360도 사이로 보정)
     */
    private Double normalizeDirection(Double d) {
        return (d % 360 + 360) % 360;
    }
}