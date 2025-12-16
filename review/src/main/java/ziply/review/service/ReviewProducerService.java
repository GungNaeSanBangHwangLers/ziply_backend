package ziply.review.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ziply.review.event.HouseCreatedEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewProducerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC_HOUSE_CREATED = "house-created";

    public void sendHouseCreatedEvent(HouseCreatedEvent event) {
        log.info("[Review] HouseCreatedEvent 발송 시작. HouseId: {}", event.getHouseId());

        kafkaTemplate.send(TOPIC_HOUSE_CREATED, String.valueOf(event.getHouseId()), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[Review] 발송 성공. Offset: {}", result.getRecordMetadata().offset());
                    } else {
                        log.error("[Review] 발송 실패. HouseId: {}", event.getHouseId(), ex);
                    }
                });
    }
}