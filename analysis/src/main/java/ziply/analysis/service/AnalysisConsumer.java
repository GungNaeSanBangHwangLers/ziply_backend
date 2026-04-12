package ziply.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ziply.analysis.event.DemoDataResetEvent;
import ziply.analysis.event.HouseCreatedEvent;
import ziply.analysis.event.HouseUpdatedEvent;
import ziply.analysis.repository.HouseRouteAnalysisRepository;

@Component
@Slf4j
@RequiredArgsConstructor
public class AnalysisConsumer {

    private final HouseRouteAnalysisRepository houseRouteAnalysisRepository;
    private final RouteAnalysisService routeAnalysisService;
    private final DemoAnalysisService demoAnalysisService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @KafkaListener(topics = "house-created", groupId = "analysis-group")
    public void handleHouseCreatedEvent(String message) {
        try {
            HouseCreatedEvent event = objectMapper.readValue(message, HouseCreatedEvent.class);
            log.info("[Analyst] 생성 이벤트 수신 성공: {}", event);
            routeAnalysisService.processHouseCreation(event);
        } catch (Exception e) {
            log.error("[Analyst] 생성 메시지 파싱 에러: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "house-location-changed", groupId = "analysis-group")
    public void handleHouseUpdatedEvent(String message) {
        try {
            HouseUpdatedEvent event = objectMapper.readValue(message, HouseUpdatedEvent.class);
            log.info("[Analyst] 수정 이벤트 수신 성공: {}", event);
            routeAnalysisService.processHouseUpdate(event);
        } catch (Exception e) {
            log.error("[Analyst] 수정 메시지 파싱 에러: {}", e.getMessage());
        }
    }

    @Transactional
    @KafkaListener(topics = "house-deleted", groupId = "analysis-group")
    public void handleHouseDeleted(String houseIdStr) {
        String cleanId = houseIdStr.replace("\"", "");

        try {
            Long houseId = Long.parseLong(cleanId);
            log.info("[Analyst] 삭제 이벤트 수신 성공 - HouseId: {}", houseId);

            houseRouteAnalysisRepository.deleteByHouseId(houseId);
            log.info("[Analyst] 분석 데이터 삭제 완료 - HouseId: {}", houseId);
        } catch (NumberFormatException e) {
            log.error("[Analyst] 숫자 변환 실패. 원본 데이터: {}, 정제된 데이터: {}", houseIdStr, cleanId);
        }
    }

    @Transactional
    @KafkaListener(topics = "search-card-deleted", groupId = "analysis-group")
    public void handleCardDeleted(String cardIdStr) {
        String cleanId = cardIdStr.replace("\"", "");

        try {
            UUID searchCardId = UUID.fromString(cleanId);
            log.info("[Analyst] 카드 삭제 이벤트 수신 - CardId: {}", searchCardId);

            houseRouteAnalysisRepository.deleteBySearchCardId(searchCardId);

            log.info("[Analyst] 해당 카드의 모든 분석 데이터 삭제 완료 - CardId: {}", searchCardId);
        } catch (Exception e) {
            log.error("[Analyst] 카드 ID 변환 실패: {}", cleanId);
        }
    }

    @Transactional
    @KafkaListener(topics = "demo-data-reset", groupId = "analysis-group")
    public void handleDemoDataReset(String message) {
        try {
            DemoDataResetEvent event = objectMapper.readValue(message, DemoDataResetEvent.class);
            log.info("[DEMO-Analysis] 데모 초기화 이벤트 수신 - userId: {}, houseIds: {}", 
                    event.getUserId(), event.getHouseIds());
            
            demoAnalysisService.resetDemoAnalysisData(event.getHouseIds());
            
            log.info("[DEMO-Analysis] 데모 초기화 처리 완료 - userId: {}", event.getUserId());
        } catch (Exception e) {
            log.error("[DEMO-Analysis] 데모 초기화 처리 실패: {}", e.getMessage(), e);
        }
    }
}
