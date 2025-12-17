package ziply.analysis.service.bus;

import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Service
public class DataCheckService {
    private final DataSource dataSource;

    public DataCheckService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean isDataAlreadyLoaded(String tableName) {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                // 행의 개수가 0보다 크면 true 반환
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("데이터 확인 중 DB 오류 발생 (" + tableName + "): " + e.getMessage());
            return false;
        }
        return false;
    }
}