package ziply.analysis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bus_stop_location")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusStopLocation {

    @Id
    @Column(name = "stops_id", length = 20)
    private String stopsId; // NODE_ID

    @Column(nullable = false)
    private Double latitude; // YCRD

    @Column(nullable = false)
    private Double longitude; // XCRD

    @Column(name = "stops_nm", length = 100)
    private String stopsNm;

    @Column(name = "stops_no", length = 20)
    private String stopsNo;
}