package ziply.analysis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ziply.analysis.domain.HouseInfrastructure;
import ziply.analysis.repository.HouseInfrastructureRepository;

@Service
@RequiredArgsConstructor
public class NoiseScoringService {

    private final JdbcTemplate jdbcTemplate;
    private final HouseInfrastructureRepository infrastructureRepository;

    // 기획안 기반 상수
    private static final double BUS_DAY_MAX = 150.0;
    private static final double BUS_NIGHT_MAX = 15.0;
    private static final double FOOD_DAY_MAX = 18.0;
    private static final double BAR_NIGHT_MAX = 8.0;
    private static final double SCHOOL_MAX = 3.0;
    private static final double SUBWAY_GROUND_MAX = 3.0;

    public int calculateDayNoiseScore(Long houseId, double lat, double lon) {
        HouseInfrastructure infra = getInfra(houseId);

        double trafficNoise = 0.5;
        double busNorm = calculateBusNorm(lat, lon, "bus_opr_total", BUS_DAY_MAX);
        double foodNorm = Math.min(infra.getRestaurantCount() / FOOD_DAY_MAX, 1.0);
        double schoolNorm = Math.min(infra.getSchoolCount() / SCHOOL_MAX, 1.0);
        double subwayNorm = Math.min(infra.getSubwayCount() / SUBWAY_GROUND_MAX, 1.0);

        double finalScore = (0.35 * trafficNoise) + (0.25 * busNorm) +
                (0.25 * foodNorm) + (0.10 * schoolNorm) +
                (0.05 * subwayNorm);

        return (int) Math.round(finalScore * 100);
    }

    public int calculateNightNoiseScore(Long houseId, double lat, double lon) {
        HouseInfrastructure infra = getInfra(houseId);

        double busNightNorm = calculateBusNorm(lat, lon, "bus_opr_23", BUS_NIGHT_MAX);
        double trafficNightNoise = 0.5;
        double barNorm = Math.min(infra.getRestaurantCount() / BAR_NIGHT_MAX, 1.0);

        double finalScore = (0.35 * busNightNorm) + (0.25 * trafficNightNoise) + (0.40 * barNorm);

        return (int) Math.round(finalScore * 100);
    }

    private double calculateBusNorm(double lat, double lon, String column, double max) {
        String sql = String.format("""
            SELECT SUM(o.%s) 
            FROM bus_stop_location s
            JOIN bus_operations o ON s.stops_id = o.stops_id
            WHERE (6371 * acos(cos(radians(?)) * cos(radians(s.latitude)) 
                  * cos(radians(s.longitude) - radians(?)) 
                  + sin(radians(?)) * sin(radians(s.latitude)))) <= 0.5
        """, column);

        Long count = jdbcTemplate.queryForObject(sql, Long.class, lat, lon, lat);
        return (count == null) ? 0.0 : Math.min(count / max, 1.0);
    }

    private HouseInfrastructure getInfra(Long houseId) {
        return infrastructureRepository.findByHouseId(houseId)
                .orElse(new HouseInfrastructure(houseId, 0, 0, 0));
    }
}