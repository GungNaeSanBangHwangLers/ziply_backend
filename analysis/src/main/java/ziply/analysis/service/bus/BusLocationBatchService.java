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

@Service
public class BusLocationBatchService {
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public BusLocationBatchService(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @Value("${bus.location.data.path}")
    private String jsonFilePath;

    private static final int BATCH_SIZE = 11295;

    public void loadBusLocationData() {
        System.out.println("Bus location data loading started from: " + jsonFilePath);

        try {
            // 1. JSON 파일 로드 및 파싱
            File file = new File(jsonFilePath);
            if (!file.exists()) {
                System.err.println("Error: JSON file not found at " + jsonFilePath);
                return;
            }

            Map<String, Object> root = objectMapper.readValue(file, Map.class);
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) root.get("DATA");

            if (dataList == null || dataList.isEmpty()) {
                System.out.println("JSON 파일에 데이터가 없거나 'DATA' 키를 찾을 수 없습니다.");
                return;
            }

            try (Connection conn = dataSource.getConnection()) {

                conn.setAutoCommit(false);

                String sql = "INSERT IGNORE INTO bus_stop_location "
                        + "(stops_id, latitude, longitude, stops_nm, stops_no) VALUES (?, ?, ?, ?, ?)";

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
                                System.out.println("Batch executed. Total rows processed: " + count);
                            }
                        } catch (NumberFormatException | NullPointerException e) {
                            System.err.printf("Skipping row due to parsing error: %s. Error: %s%n", row,
                                    e.getMessage());
                        }
                    }
                    pstmt.executeBatch();
                    conn.commit();

                    System.out.println("Data loading complete! Total rows inserted: " + count);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database Error (SQL): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("File/Parsing Error (I/O or JSON): " + e.getMessage());
        }
    }
}