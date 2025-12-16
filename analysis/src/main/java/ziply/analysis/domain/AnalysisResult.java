package ziply.analysis.domain;

import jakarta.persistence.*;
import lombok.*;

// ... (다른 Import 생략)

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long houseId;

    private Double latitude;
    private Double longitude;

    private int wlbScore;

    private int noiseScore;

    @Builder
    public AnalysisResult(Long houseId, Double latitude, Double longitude,
                          int wlbScore, int noiseScore) {

        this.houseId = houseId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.wlbScore = wlbScore;
        this.noiseScore = noiseScore;
    }
}