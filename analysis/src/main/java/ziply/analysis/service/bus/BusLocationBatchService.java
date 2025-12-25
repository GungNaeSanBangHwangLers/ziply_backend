package ziply.analysis.service.bus;

import com.fasterxml.jackson.databind.ObjectMapper;
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
public class BusLocationBatchService {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 5000;

    public void loadBusLocationData() {
        // 위치 데이터는 seoulBusCount.json에 들어 있음
        ClassPathResource resource = new ClassPathResource("seoulBusCount.json");

        try (InputStream is = resource.getInputStream()) {
            Map<String, Object> root = objectMapper.readValue(is, Map.class);
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) root.get("DATA");

            if (dataList == null || dataList.isEmpty()) {
                log.warn("No bus location data found in JSON.");
                return;
            }

            log.info("Starting Batch Insert for Bus Locations. Total data size: {}", dataList.size());
            executeBatchInsert(dataList);

        } catch (Exception e) {
            log.error("Error occurred while loading bus location data.", e);
        }
    }

    private void executeBatchInsert(List<Map<String, Object>> dataList) throws SQLException {
        String sql = "INSERT IGNORE INTO bus_stop_location (stops_id, latitude, longitude, stops_nm, stops_no) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int count = 0;
                for (Map<String, Object> row : dataList) {
                    try {
                        pstmt.setString(1, getValStr(row, "NODE_ID"));
                        pstmt.setDouble(2, parseToDouble(row.get("YCRD")));
                        pstmt.setDouble(3, parseToDouble(row.get("XCRD")));
                        pstmt.setString(4, getValStr(row, "STOPS_NM"));
                        pstmt.setString(5, getValStr(row, "STOPS_NO"));

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
                log.info("Bus location data loading completed. Total rows inserted: {}", count);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private String getValStr(Map<String, Object> row, String key) {
        Object val = row.get(key.toUpperCase());
        if (val == null) val = row.get(key.toLowerCase());
        return val != null ? val.toString() : null;
    }

    private double parseToDouble(Object value) {
        if (value == null) return 0.0;
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }
}