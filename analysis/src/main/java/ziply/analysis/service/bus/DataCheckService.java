package ziply.analysis.service.bus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataCheckService {

    private final DataSource dataSource;

    public boolean isDataAlreadyLoaded(String type) {
        // 1. 입력받은 type을 실제 DB 테이블명으로 변환 (매핑 로직 호출)
        String tableName = getActualTableName(type);
        String sql = "SELECT COUNT(*) FROM " + tableName;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                int count = rs.getInt(1);
                log.info("[DataCheck] Type: '{}' -> Table: '{}' | Count: {}", type, tableName, count);
                return count > 0;
            }
        } catch (SQLException e) {
            // 테이블이 없거나 쿼리 실패 시 로그를 남기고 false 반환 (배치 실행 유도)
            log.warn("[DataCheck] Cannot check table '{}'. It might not exist yet. Error: {}", tableName, e.getMessage());
        }
        return false;
    }

    private String getActualTableName(String type) {
        return switch (type) {
            case "cctvs" -> "cctv";           // 'cctvs' 요청 시 실제 테이블명 'cctv' 사용
            case "police_stations" -> "police_stations";
            case "street_lights" -> "street_lights";
            case "bus_stop_location" -> "bus_stop_location";
            case "bus_operations" -> "bus_operations";
            case "bus_stop_stats" -> "bus_stop_stats";
            default -> type;                  // 그 외에는 입력값 그대로 사용
        };
    }
}