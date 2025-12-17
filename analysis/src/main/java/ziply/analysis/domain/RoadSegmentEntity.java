package ziply.analysis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "road_segment_geometry") // T3 테이블
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoadSegmentEntity {

    // 도로 구간 ID (API의 LINK_ID). T4와 관계를 맺는 Primary Key
    @Id
    @Column(name = "segment_id", length = 10, nullable = false)
    private String segmentId;

    // 도로 구분 (간선도로, 보조간선도로 등). 혼잡도 기준 속도 계산에 사용됨
    @Column(name = "road_div_nm", length = 20)
    private String roadDivNm;

    @Column(name = "start_node_nm", length = 100)
    private String startNodeNm; // 시점 명칭

    @Column(name = "end_node_nm", length = 100)
    private String endNodeNm; // 종점 명칭

    @Builder
    public RoadSegmentEntity(String segmentId, String roadDivNm, String startNodeNm, String endNodeNm) {
        this.segmentId = segmentId;
        this.roadDivNm = roadDivNm;
        this.startNodeNm = startNodeNm;
        this.endNodeNm = endNodeNm;
    }
}