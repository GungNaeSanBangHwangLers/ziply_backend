package ziply.auth.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ziply.auth.exception.AuthException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<?> handleAuthException(AuthException e) {
        return ResponseEntity.status(401)
                .body(Map.of(
                        "code", "AUTH_FAILED",
                        "message", "로그인에 실패했습니다."
                ));
    }
}
