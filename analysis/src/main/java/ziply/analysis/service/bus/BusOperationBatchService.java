package ziply.analysis.service.bus;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Statement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusOperationBatchService {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 5000;

    public void loadBusOperationData() {
        // 운행 횟수 데이터는 seoulBusPosition.json에 들어 있음
        ClassPathResource resource = new ClassPathResource("seoulBusPosition.json");

        try (InputStream is = resource.getInputStream()) {
            Map<String, Object> root = objectMapper.readValue(is, Map.class);
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) root.get("DATA");

            if (dataList == null || dataList.isEmpty()) {
                log.warn("No bus operation data found in JSON.");
                return;
            }

            log.info("Starting Batch Insert for Bus Operations. Total data size: {}", dataList.size());
            executeBatchInsert(dataList);

        } catch (Exception e) {
            log.error("Failed to load bus operation data.", e);
        }
    }

    private void executeBatchInsert(List<Map<String, Object>> dataList) throws SQLException {
        String sql = "INSERT IGNORE INTO bus_operations (stops_id, route_id, bus_opr_day, bus_opr_night) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int count = 0;

                for (Map<String, Object> row : dataList) {
                    try {
                        String stopsId = getValStr(row, "STOPS_ID");
                        String routeId = getValStr(row, "RTE_ID");

                        if (stopsId == null || routeId == null) {
                            continue;
                        }

                        int dayTotal = 0;
                        for (int h = 6; h <= 22; h++) {
                            dayTotal += getValInt(row, String.format("BUS_OPR_%02d", h));
                        }

                        int nightTotal = 0;
                        nightTotal += getValInt(row, "BUS_OPR_23");
                        for (int h = 0; h <= 5; h++) {
                            nightTotal += getValInt(row, String.format("BUS_OPR_%02d", h));
                        }

                        pstmt.setString(1, stopsId);
                        pstmt.setString(2, routeId);
                        pstmt.setInt(3, dayTotal);
                        pstmt.setInt(4, nightTotal);

                        pstmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            pstmt.executeBatch();
                            conn.commit();
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }

                pstmt.executeBatch();
                conn.commit();
                log.info("Bus operation data loading completed. Total rows inserted: {}", count);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private int getValInt(Map<String, Object> row, String key) {
        Object val = row.get(key.toUpperCase());
        if (val == null) {
            val = row.get(key.toLowerCase());
        }
        return parseToInt(val);
    }

    private String getValStr(Map<String, Object> row, String key) {
        Object val = row.get(key.toUpperCase());
        if (val == null) {
            val = row.get(key.toLowerCase());
        }
        return val != null ? val.toString() : null;
    }

    private int parseToInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return (int) Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    public void updateBusStopStats() {
        String sql = "INSERT INTO bus_stop_stats (stops_id, day_opr_sum, night_opr_sum) " +
                "SELECT stops_id, SUM(bus_opr_day), SUM(bus_opr_night) " +
                "FROM bus_operations " +
                "GROUP BY stops_id " +
                "ON DUPLICATE KEY UPDATE " +
                "day_opr_sum = VALUES(day_opr_sum), " +
                "night_opr_sum = VALUES(night_opr_sum)";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            long startTime = System.currentTimeMillis();
            stmt.executeUpdate(sql);
            long endTime = System.currentTimeMillis();

            log.info("Bus stop statistics calculation completed in {} ms.", (endTime - startTime));
        } catch (SQLException e) {
            log.error("Failed to update bus stop statistics", e);
        }
    }
}