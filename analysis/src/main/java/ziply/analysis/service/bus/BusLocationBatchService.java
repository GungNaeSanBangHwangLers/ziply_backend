package ziply.analysis.service.bus;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
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

    @Value("${bus.location.data.path}")
    private String jsonFilePath;

    private static final int BATCH_SIZE = 5000;

    public void loadBusLocationData() {
        File file = new File(jsonFilePath);
        if (!file.exists()) {
            log.error("Bus location JSON file not found at: {}", jsonFilePath);
            return;
        }

        try {
            Map<String, Object> root = objectMapper.readValue(file, Map.class);
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) root.get("DATA");

            if (dataList == null || dataList.isEmpty()) {
                log.warn("No bus location data found in JSON.");
                return;
            }

            executeBatchInsert(dataList);

        } catch (Exception e) {
            log.error("Error occurred while loading bus location data", e);
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
                        pstmt.setString(1, (String) row.get("node_id"));
                        pstmt.setDouble(2, Double.parseDouble((String) row.get("ycrd")));
                        pstmt.setDouble(3, Double.parseDouble((String) row.get("xcrd")));
                        pstmt.setString(4, (String) row.get("stops_nm"));
                        pstmt.setString(5, (String) row.get("stops_no"));

                        pstmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            pstmt.executeBatch();
                            conn.commit();
                            log.info("Bus location batch committed: {} rows", count);
                        }
                    } catch (NumberFormatException | NullPointerException e) {
                        log.warn("Skipping row due to invalid data: {}. Error: {}", row, e.getMessage());
                    }
                }

                pstmt.executeBatch();
                conn.commit();
                log.info("Bus location data loading completed. Total rows: {}", count);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}