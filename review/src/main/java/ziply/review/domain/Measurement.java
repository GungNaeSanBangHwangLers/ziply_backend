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

    @Builder
    public Measurement(House house, Integer round, Double direction, Double lightLevel) {
        this.house = house;
        this.round = round;
        this.direction = direction;
        this.lightLevel = lightLevel;
    }

    public void assignHouse(House house) {
        this.house = house;
        if (!house.getMeasurements().contains(this)) {
            house.getMeasurements().add(this);
        }
    }

    public void updateDirection(Double direction) {
        if (direction != null) {
            if (direction < 0 || direction > 360) {
                this.direction = direction % 360;
            } else {
                this.direction = direction;
            }
        }
    }

    public void updateLightLevel(Double lightLevel) {
        if (lightLevel != null) {
            this.lightLevel = lightLevel;
        }
    }
}