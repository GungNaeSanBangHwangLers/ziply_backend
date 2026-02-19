package ziply.analysis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bus_operations", indexes = {
        @Index(name = "idx_stops_route", columnList = "stops_id, route_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stops_id", length = 20, nullable = false)
    private String stopsId;

    @Column(name = "route_id", length = 20, nullable = false)
    private String routeId;

    @Column(name = "bus_opr_day")
    private Integer busOprDay;

    @Column(name = "bus_opr_night")
    private Integer busOprNight;
}