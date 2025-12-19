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
public class BusOperationBatchService {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 5000;

    @Value("${bus.location.data.path1}")
    private String jsonFilePath;

    public void loadBusOperationData() {
        File file = new File(jsonFilePath);
        if (!file.exists()) {
            log.error("Bus operation JSON file not found at: {}", jsonFilePath);
            return;
        }

        try {
            Map<String, Object> root = objectMapper.readValue(file, Map.class);
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) root.get("DATA");

            if (dataList == null || dataList.isEmpty()) {
                log.warn("No data found in JSON file: {}", jsonFilePath);
                return;
            }

            executeBatchInsert(dataList);

        } catch (Exception e) {
            log.error("Failed to load bus operation data", e);
        }
    }

    private void executeBatchInsert(List<Map<String, Object>> dataList) throws SQLException {
        // 테이블 컬럼명에 맞춰 INSERT 쿼리 작성
        String sql = "INSERT IGNORE INTO bus_operations (stops_id, route_id, bus_opr_day, bus_opr_night) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int count = 0;

                for (Map<String, Object> row : dataList) {
                    try {
                        // ID 필드 대소문자 방어 로직
                        String stopsId = getValStr(row, "stops_id");
                        String routeId = getValStr(row, "rte_id");

                        if (stopsId == null || routeId == null) continue;

                        int dayTotal = 0;
                        for (int h = 6; h <= 22; h++) {
                            dayTotal += getValInt(row, String.format("bus_opr_%02d", h));
                        }

                        int nightTotal = 0;
                        nightTotal += getValInt(row, "bus_opr_23"); // 23시
                        for (int h = 0; h <= 5; h++) { // 00시 ~ 05시
                            nightTotal += getValInt(row, String.format("bus_opr_%02d", h));
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
                            log.info("Bus operation batch committed: {} rows", count);
                        }
                    } catch (Exception e) {
                        log.warn("Skipping row due to error: {}", e.getMessage());
                    }
                }

                pstmt.executeBatch();
                conn.commit();
                log.info("Bus operation data loading completed. Total rows: {}", count);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * 키값이 소문자든 대문자든 상관없이 숫자를 가져오는 메서드
     */
    private int getValInt(Map<String, Object> row, String key) {
        Object val = row.get(key.toLowerCase());
        if (val == null) val = row.get(key.toUpperCase());
        return parseToInt(val);
    }

    /**
     * 키값이 소문자든 대문자든 상관없이 문자열(ID)을 가져오는 메서드
     */
    private String getValStr(Map<String, Object> row, String key) {
        Object val = row.get(key.toLowerCase());
        if (val == null) val = row.get(key.toUpperCase());
        return val != null ? val.toString() : null;
    }

    private int parseToInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            // String으로 들어올 경우 "5.0" 같은 소수점 처리 포함
            return (int) Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}