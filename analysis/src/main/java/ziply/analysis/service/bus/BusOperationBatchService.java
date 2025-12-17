package ziply.analysis.service.bus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class BusOperationBatchService {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    // ⭐️ 추가: 5000개 단위로 커밋할 배치 크기 정의
    private static final int RECOMMENDED_BATCH_SIZE = 5000;

    @Value("${bus.location.data.path1}")
    private String jsonFilePath;

    public BusOperationBatchService(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    public void loadBusOperationData() {
        System.out.println("Bus operation data loading started from: " + jsonFilePath);

        File file = new File(jsonFilePath);
        if (!file.exists()) {
            System.err.println("Error: JSON file not found at " + jsonFilePath);
            return;
        }

        try {
            // 1. JSON 파일을 메모리에 한 번에 로드 (기존 방식 유지)
            Map<String, Object> root = objectMapper.readValue(file, Map.class);
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) root.get("DATA");

            if (dataList == null || dataList.isEmpty()) {
                System.out.println("JSON 파일에 데이터가 없거나 'DATA' 키를 찾을 수 없습니다.");
                return;
            }

            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);

                // 외래 키 제약 조건 일시 해제 로직은 필요에 따라 주석 해제하여 사용
                // try (java.sql.Statement stmt = conn.createStatement()) {
                //     stmt.execute("SET FOREIGN_KEY_CHECKS = 0;");
                // }

                String sql = "INSERT IGNORE INTO bus_operations " +
                        "(stops_id, route_id, bus_opr_total, bus_opr_23) VALUES (?, ?, ?, ?)";

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    int count = 0;

                    for (Map<String, Object> row : dataList) {
                        try {
                            String stopsId = Objects.toString(row.get("stops_id"), null);
                            String routeId = Objects.toString(row.get("rte_id"), null);

                            Integer totalOpr = (Integer) row.get("bus_opr");
                            Integer nightOpr = (Integer) row.get("bus_opr_23");

                            if (stopsId == null || stopsId.isEmpty() || routeId == null || routeId.isEmpty()) {
                                throw new NullPointerException("필수 키(stops_id 또는 rte_id) 누락 또는 빈 값");
                            }

                            pstmt.setString(1, stopsId);
                            pstmt.setString(2, routeId);
                            pstmt.setInt(3, totalOpr != null ? totalOpr : 0);
                            pstmt.setInt(4, nightOpr != null ? nightOpr : 0);

                            pstmt.addBatch();
                            count++;

                            // ⭐️ 로깅 추가
                            // System.out.printf("LOG BATCH ADD: stops_id=%s, route_id=%s, totalOpr=%d%n", stopsId, routeId, (totalOpr != null ? totalOpr : 0));

                            // ⭐️ 5000개 단위로 배치 실행 및 커밋
                            if (count % RECOMMENDED_BATCH_SIZE == 0) {
                                pstmt.executeBatch();
                                conn.commit();
                                System.out.printf("====================================================%n");
                                System.out.printf("✅ [BATCH COMMIT] %d rows processed and committed.%n", count);
                                System.out.printf("====================================================%n");
                            }


                        } catch (NullPointerException e) {
                            System.err.printf("⚠️ Skipping row (NULL/Empty): %s. Error: %s%n", row, e.getMessage());
                        } catch (ClassCastException e) {
                            System.err.printf("❌ Skipping row (Casting Error): %s. Error: %s%n", row, e.getMessage());
                        }
                    }

                    // 마지막 남은 배치 실행 및 최종 커밋
                    if (count % RECOMMENDED_BATCH_SIZE != 0 || count == 0) {
                        pstmt.executeBatch();
                        conn.commit();
                    }

                    // 외래 키 제약 조건 복구
                    // try (java.sql.Statement stmt = conn.createStatement()) {
                    //     stmt.execute("SET FOREIGN_KEY_CHECKS = 1;");
                    // }

                    System.out.println("✅ Data loading complete! Total rows inserted: " + count);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database Error (SQL): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("File/Parsing Error (I/O or JSON): " + e.getMessage());
            e.printStackTrace();
        }
    }
}