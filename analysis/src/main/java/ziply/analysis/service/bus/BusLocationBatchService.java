package ziply.analysis.service.bus;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusLocationBatchService {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public void loadBusLocationData() {
        ClassPathResource resource = new ClassPathResource("seoulBusCount.json");
        String sql = "INSERT INTO bus_stop_location (stops_id, latitude, longitude, stops_nm, stops_no) " +
                "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE stops_nm = VALUES(stops_nm)";

        try (InputStream is = resource.getInputStream();
             Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            JsonParser parser = objectMapper.getFactory().createParser(is);

            // [수정] DATA 배열이 시작될 때까지 모든 토큰을 탐색합니다.
            boolean foundData = false;
            while (parser.nextToken() != null) {
                String fieldName = parser.getCurrentName();
                if ("DATA".equals(fieldName) && parser.currentToken() == JsonToken.START_ARRAY) {
                    foundData = true;
                    break;
                }
            }

            if (!foundData) {
                log.error("JSON 내에서 'DATA' 배열을 찾을 수 없습니다.");
                return;
            }

            int count = 0;
            // [수정] START_OBJECT 토큰을 만날 때마다 하나씩 읽어 들입니다.
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                Map<String, Object> row = objectMapper.readValue(parser, Map.class);

                // [수정] getValStr를 사용하여 대소문자 무관하게 값을 가져옵니다.
                pstmt.setString(1, getValStr(row, "node_id"));
                pstmt.setDouble(2, parseToDouble(getValStr(row, "ycrd")));
                pstmt.setDouble(3, parseToDouble(getValStr(row, "xcrd")));
                pstmt.setString(4, getValStr(row, "stops_nm"));
                pstmt.setString(5, getValStr(row, "stops_no"));
                pstmt.addBatch();

                if (++count % 1000 == 0) {
                    pstmt.executeBatch();
                    conn.commit();
                    log.info("Bus Location Progress: {} rows inserted...", count);
                }
            }

            pstmt.executeBatch();
            conn.commit();
            log.info("Bus Location Load Completed! Total: {} rows.", count);

        } catch (Exception e) {
            log.error("Batch insert failed for bus locations", e);
        }
    }

    private String getValStr(Map<String, Object> row, String key) {
        if (row == null) return null;
        Object val = row.get(key.toLowerCase());
        if (val == null) val = row.get(key.toUpperCase());
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