package ziply.analysis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "road_traffic") // T4 테이블
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoadTrafficEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // T3의 segment_id와 매핑되는 Foreign Key
    @Column(name = "segment_id", length = 10, nullable = false)
    private String segmentId;

    // API에서 가져온 평균 속도 (AVG_SPD)
    @Column(name = "avg_spd", nullable = false)
    private Double avgSpd;

    // API에서 가져온 시간 코드 (TIME_CD). 주간/야간 소음 구분에 사용됨
    @Column(name = "time_code", length = 5, nullable = false)
    private String timeCode;

    // 데이터 수집 시각
    @Column(name = "collect_time", nullable = false)
    private LocalDateTime collectTime;

    @Builder
    public RoadTrafficEntity(String segmentId, Double avgSpd, String timeCode, LocalDateTime collectTime) {
        this.segmentId = segmentId;
        this.avgSpd = avgSpd;
        this.timeCode = timeCode;
        this.collectTime = collectTime;
    }
}