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

    public boolean isDataAlreadyLoaded(String tableName) {
        String sql = "SELECT COUNT(*) FROM " + tableName;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                int count = rs.getInt(1);
                log.debug("Table '{}' record count: {}", tableName, count);
                return count > 0;
            }
        } catch (SQLException e) {
            log.error("Failed to check data existence in table: {}", tableName, e);
        }
        return false;
    }
}