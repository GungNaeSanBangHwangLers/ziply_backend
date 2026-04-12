package ziply.review.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ziply.review.event.DemoDataResetEvent;
import ziply.review.event.HouseCreatedEvent;
import ziply.review.event.HouseUpdatedEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewProducerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_HOUSE_CREATED = "house-created";
    private static final String TOPIC_HOUSE_UPDATED = "house-location-changed";
    private static final String TOPIC_HOUSE_DELETE = "house-deleted";
    private static final String TOPIC_DEMO_RESET = "demo-data-reset";

    public void sendHouseCreatedEvent(HouseCreatedEvent event) {
        log.info("[Review] HouseCreatedEvent 발송 시작. HouseId: {}", event.getHouseId());

        kafkaTemplate.send(TOPIC_HOUSE_CREATED, String.valueOf(event.getHouseId()), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[Review] 카프카 메시지 송신 - event: {}", event);
                    } else {
                        log.error("[Review] 발송 실패. HouseId: {}", event.getHouseId(), ex);
                    }
                });
    }

    public void sendHouseUpdatedEvent(HouseUpdatedEvent event) {
        log.info("[Review] HouseUpdatedEvent 발송 시작. HouseId: {}, New Address: {}", event.getHouseId(),
                event.getAddress());

        kafkaTemplate.send(TOPIC_HOUSE_UPDATED, String.valueOf(event.getHouseId()), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[Review] 카프카 메시지 송신(Update) - event: {}", event);
                    } else {
                        log.error("[Review] 수정 이벤트 발송 실패. HouseId: {}", event.getHouseId(), ex);
                    }
                });
    }

    public void sendDeleteSignal(Long houseId) {
        kafkaTemplate.send(TOPIC_HOUSE_DELETE, String.valueOf(houseId), houseId.toString());
        log.info("[Review] 삭제 신호 보냄 - HouseId: {}", houseId);
    }

    public void sendCardDeletedEvent(UUID cardId) {
        kafkaTemplate.send("search-card-deleted", cardId.toString(), cardId.toString());
        log.info("[Review] 탐색카드 삭제 신호 보냄 - CardId: {}", cardId);
    }

    public void sendDemoDataResetEvent(DemoDataResetEvent event) {
        log.info("[Review] DemoDataResetEvent 발송 시작. UserId: {}", event.getUserId());

        kafkaTemplate.send(TOPIC_DEMO_RESET, String.valueOf(event.getUserId()), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[Review] 데모 초기화 이벤트 발송 완료 - UserId: {}", event.getUserId());
                    } else {
                        log.error("[Review] 데모 초기화 이벤트 발송 실패 - UserId: {}", event.getUserId(), ex);
                    }
                });
    }
}
