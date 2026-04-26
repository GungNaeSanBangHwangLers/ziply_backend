package ziply.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "house-created", groupId = "analysis-group")
    public void handleHouseCreatedEvent(String message) {
        try {
            HouseCreatedEvent event = objectMapper.readValue(message, HouseCreatedEvent.class);
            log.info("집 생성 이벤트 수신: {}", event);
            routeAnalysisService.processHouseCreation(event);
        } catch (Exception e) {
            log.error("집 생성 메시지 처리 실패: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "house-location-changed", groupId = "analysis-group")
    public void handleHouseUpdatedEvent(String message) {
        try {
            HouseUpdatedEvent event = objectMapper.readValue(message, HouseUpdatedEvent.class);
            log.info("집 수정 이벤트 수신: {}", event);
            routeAnalysisService.processHouseUpdate(event);
        } catch (Exception e) {
            log.error("집 수정 메시지 처리 실패: {}", e.getMessage(), e);
        }
    }

    @Transactional
    @KafkaListener(topics = "house-deleted", groupId = "analysis-group")
    public void handleHouseDeleted(String houseIdStr) {
        String cleanId = houseIdStr.replace("\"", "");
        try {
            Long houseId = Long.parseLong(cleanId);
            log.info("집 삭제 이벤트 수신: houseId={}", houseId);
            houseRouteAnalysisRepository.deleteByHouseId(houseId);
            log.info("집 분석 데이터 삭제 완료: houseId={}", houseId);
        } catch (NumberFormatException e) {
            log.error("집 삭제 이벤트 houseId 파싱 실패: raw={}, clean={}", houseIdStr, cleanId, e);
        }
    }

    @Transactional
    @KafkaListener(topics = "search-card-deleted", groupId = "analysis-group")
    public void handleCardDeleted(String cardIdStr) {
        String cleanId = cardIdStr.replace("\"", "");
        try {
            UUID searchCardId = UUID.fromString(cleanId);
            log.info("탐색카드 삭제 이벤트 수신: searchCardId={}", searchCardId);
            houseRouteAnalysisRepository.deleteBySearchCardId(searchCardId);
            log.info("탐색카드 분석 데이터 삭제 완료: searchCardId={}", searchCardId);
        } catch (Exception e) {
            log.error("탐색카드 삭제 이벤트 ID 파싱 실패: raw={}", cleanId, e);
        }
    }

    @Transactional
    @KafkaListener(topics = "demo-data-reset", groupId = "analysis-group")
    public void handleDemoDataReset(String message) {
        try {
            DemoDataResetEvent event = objectMapper.readValue(message, DemoDataResetEvent.class);
            log.info("데모 초기화 이벤트 수신: userId={}, houseIds={}", event.getUserId(), event.getHouseIds());
            demoAnalysisService.resetDemoAnalysisData(event.getHouseIds());
            log.info("데모 초기화 완료: userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("데모 초기화 처리 실패: {}", e.getMessage(), e);
        }
    }
}
