package ziply.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ziply.review.dto.response.DemoResetResponse;
import ziply.review.service.DemoService;

@Slf4j
@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
@Tag(name = "Exhibition Demo", description = "전시회 데모 관리 API")
public class DemoController {

    private final DemoService demoService;

    @DeleteMapping("/reset")
    @Operation(
        summary = "데모 데이터 초기화", 
        description = "전시회용 데모 계정의 모든 리뷰/분석 데이터를 초기화합니다. JWT 토큰 필요."
    )
    public ResponseEntity<DemoResetResponse> resetDemoData(
            @RequestHeader(value = "X-User-Id", required = true) Long userId) {
        
        log.info("[DEMO] 데모 데이터 초기화 요청 - userId: {}", userId);
        
        DemoResetResponse response = demoService.resetDemoData(userId);
        
        log.info("[DEMO] 데모 데이터 초기화 완료 - userId: {}, 삭제된 데이터: {}", 
                userId, response.getDeletedData());
        
        return ResponseEntity.ok(response);
    }
}
