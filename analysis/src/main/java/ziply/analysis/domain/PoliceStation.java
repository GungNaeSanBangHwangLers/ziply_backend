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
@Table(name = "police_stations")
public class PoliceStation {
    @Id
    private Long id; // 제공된 데이터의 연번 사용

    private String stationName; // 관서명
    private String classification; // 구분 (지구대/파출소)
    private Double latitude;
    private Double longitude;

    @Builder
    public PoliceStation(Long id, String stationName, String classification, Double latitude, Double longitude) {
        this.id = id;
        this.stationName = stationName;
        this.classification = classification;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}