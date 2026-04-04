package ziply.review.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GeocodingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleGeocodingFailure(GeocodingFailureException e) {
        log.warn("Geocoding 실패: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "INVALID_ADDRESS");
        response.put("message", "유효하지 않은 주소가 있어 집을 저장할 수 없습니다.");
        
        List<Map<String, Object>> invalidAddresses = e.getInvalidAddresses().stream()
                .map(info -> {
                    Map<String, Object> addressInfo = new HashMap<>();
                    addressInfo.put("index", info.getIndex() + 1);  // 1부터 시작
                    addressInfo.put("address", info.getAddress());
                    addressInfo.put("message", String.format("%d번째 주소가 유효하지 않습니다", info.getIndex() + 1));
                    return addressInfo;
                })
                .collect(Collectors.toList());
        
        response.put("invalidAddresses", invalidAddresses);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("잘못된 요청: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "INVALID_REQUEST");
        response.put("message", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException e) {
        log.warn("권한 오류: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "FORBIDDEN");
        response.put("message", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("예상치 못한 오류 발생", e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "INTERNAL_SERVER_ERROR");
        response.put("message", "서버 오류가 발생했습니다: " + e.getMessage());
        response.put("exceptionType", e.getClass().getName());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
