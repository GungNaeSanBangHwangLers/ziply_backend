package ziply.analysis.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "street_lights")
public class StreetLight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String managementNumber; // 관리번호
    private Double latitude;
    private Double longitude;

    @Builder
    public StreetLight(String managementNumber, Double latitude, Double longitude) {
        this.managementNumber = managementNumber;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}