package ziply.analysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ziply.analysis.domain.HouseInfrastructure;
import ziply.analysis.repository.HouseInfrastructureRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoiseScoringService {

    private final JdbcTemplate jdbcTemplate;
    private final HouseInfrastructureRepository infrastructureRepository;

    private static final double BUS_DAY_MAX = 1000.0;
    private static final double BUS_NIGHT_MAX = 100.0;
    private static final double FOOD_DAY_MAX = 140.0;
    private static final double SCHOOL_MAX = 3.0;
    private static final double SUBWAY_MAX = 3.0;

    private static final double SCAN_RADIUS = 0.2; // 200m

    public int calculateDayNoiseScore(Long houseId, double lat, double lon) {
        HouseInfrastructure infra = getInfra(houseId);

        double trafficNoise = estimateTrafficNoise(lat, lon, "day_opr_sum", BUS_DAY_MAX);
        double busNorm = calculateBusNorm(lat, lon, "day_opr_sum", BUS_DAY_MAX);

        double foodNorm = applyLogScale(infra.getRestaurantCount(), FOOD_DAY_MAX);
        double schoolNorm = Math.min(infra.getSchoolCount() / SCHOOL_MAX, 1.0);
        double subwayNorm = Math.min(infra.getSubwayCount() / SUBWAY_MAX, 1.0);

        double totalDayNoise = (0.35 * trafficNoise) + (0.25 * busNorm) + (0.25 * foodNorm) + (0.10 * schoolNorm) + (0.05 * subwayNorm);

        return convertToQuietnessScore(totalDayNoise);
    }

    public int calculateNightNoiseScore(Long houseId, double lat, double lon) {
        HouseInfrastructure infra = getInfra(houseId);

        double busNightNorm = calculateBusNorm(lat, lon, "night_opr_sum", BUS_NIGHT_MAX);
        double trafficNightNoise = estimateTrafficNoise(lat, lon, "night_opr_sum", BUS_NIGHT_MAX);

        double barNorm = applyLogScale(infra.getRestaurantCount(), 40.0);

        double totalNightNoise = (0.35 * busNightNorm) + (0.25 * trafficNightNoise) + (0.40 * barNorm);

        return convertToQuietnessScore(totalNightNoise);
    }

    private int convertToQuietnessScore(double danger) {
        double quietness;
        if (danger <= 0.6) {
            quietness = 1.0 - (danger * 0.5);
        } else {
            quietness = 0.7 - ((danger - 0.6) * 1.5);
        }
        int finalScore = (int) Math.round(quietness * 100);
        return Math.max(15, Math.min(100, finalScore));
    }

    private String getNoiseSql(String column) {
        return "SELECT SUM(s." + column + " / (POWER(ST_Distance(POINT(?, ?), POINT(loc.longitude, loc.latitude)) * 111139 / 30, 2) + 1)) " +
                "FROM bus_stop_stats s " +
                "JOIN bus_stop_location loc ON s.stops_id = loc.stops_id " +
                "WHERE (6371 * acos(cos(radians(?)) * cos(radians(loc.latitude)) " +
                "      * cos(radians(loc.longitude) - radians(?)) " +
                "      + sin(radians(?)) * sin(radians(loc.latitude)))) <= ?";
    }

    private double estimateTrafficNoise(double lat, double lon, String column, double max) {
        String sql = getNoiseSql(column);
        Double impact = jdbcTemplate.queryForObject(sql, Double.class, lon, lat, lat, lon, lat, SCAN_RADIUS);
        return (impact == null) ? 0.0 : Math.min(impact / (max * 0.5), 1.0);
    }

    private double calculateBusNorm(double lat, double lon, String column, double max) {
        String sql = getNoiseSql(column);
        Double count = jdbcTemplate.queryForObject(sql, Double.class, lon, lat, lat, lon, lat, SCAN_RADIUS);
        return (count == null) ? 0.0 : Math.min(count / max, 1.0);
    }

    private double applyLogScale(int count, double max) {
        if (count <= 0) return 0.0;
        return Math.min(Math.log10(count + 1) / Math.log10(max + 1), 1.0);
    }

    private HouseInfrastructure getInfra(Long houseId) {
        return infrastructureRepository.findByHouseId(houseId).orElse(new HouseInfrastructure(houseId, 0, 0, 0));
    }
}