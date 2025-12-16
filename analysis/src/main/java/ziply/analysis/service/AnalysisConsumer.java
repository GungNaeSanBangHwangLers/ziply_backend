package ziply.analysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.analysis.event.HouseCreatedEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisConsumer {
    private final RouteAnalysisService routeAnalysisService;

    @Transactional
    @KafkaListener(topics = "house-created", groupId = "analysis-group")
    public void handleHouseCreatedEvent(HouseCreatedEvent event) {
        log.info("[Analyst] 카프카 메시지 수신 - event: {}", event);
        routeAnalysisService.processHouseCreation(event);
    }
}