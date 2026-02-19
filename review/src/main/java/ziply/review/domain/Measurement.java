package ziply.review.domain;

import jakarta.persistence.*;
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
    private String imageUrl;

    @Column(length = 1000)
    private String directionFeatures;

    @Column(length = 1000)
    private String directionPros;

    @Column(length = 1000)
    private String directionCons;

    @Builder
    public Measurement(House house, Integer round, Double direction, Double lightLevel,
                       String directionType, String windowLocation, String imageUrl,
                       String directionFeatures, String directionPros, String directionCons) {
        this.house = house;
        this.round = round;
        this.direction = (direction != null) ? normalizeDirection(direction) : null;
        this.lightLevel = lightLevel;
        this.directionType = directionType;
        this.windowLocation = windowLocation;
        this.imageUrl = imageUrl;
        this.directionFeatures = directionFeatures;
        this.directionPros = directionPros;
        this.directionCons = directionCons;
    }

    public void updateDirection(Double direction) {
        if (direction != null) {
            this.direction = normalizeDirection(direction);
        }
    }

    private Double normalizeDirection(Double d) {
        return (d % 360 + 360) % 360;
    }

    public void assignHouse(House house) {
        if (this.house != null) {
            this.house.getMeasurements().remove(this);
        }
        this.house = house;
        if (house != null && !house.getMeasurements().contains(this)) {
            house.getMeasurements().add(this);
        }
    }
    public void updateLightLevel(Double lightLevel) {
        if (lightLevel != null) {
            this.lightLevel = lightLevel;
        }
    }

    public void updateDirectionInfo(Double direction, String type, String features, String pros, String cons) {
        updateDirection(direction); // 내부의 normalizeDirection 활용
        this.directionType = type;
        this.directionFeatures = features;
        this.directionPros = pros;
        this.directionCons = cons;
    }


    public void updateImageUrl(String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            this.imageUrl = imageUrl;
        }
    }
}