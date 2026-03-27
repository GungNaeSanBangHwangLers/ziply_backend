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
@Table(name = "bus_stop_stats")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusStopStats {

    @Id
    @Column(name = "stops_id", length = 20)
    private String stopsId;

    @Column(name = "day_opr_sum")
    private Integer dayOprSum;

    @Column(name = "night_opr_sum")
    private Integer nightOprSum;
}