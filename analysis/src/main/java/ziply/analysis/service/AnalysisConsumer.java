package ziply.analysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.analysis.domain.AnalysisResult;
import ziply.analysis.event.HouseCreatedEvent; // DTO 임포트
import ziply.analysis.repository.AnalysisResultRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisConsumer {
    private final AnalysisResultRepository analysisResultRepository;
    private final NoiseScoringService noiseScoringService;

    @Transactional
    @KafkaListener(topics = "house-created", groupId = "ziply-analysis-group")
    public void consume(HouseCreatedEvent event) {
        log.info("[Analysis] 카프카 메시지 수신 - HouseId: {}", event.getHouseId());

        try {
            Double lat = event.getLatitude();
            Double lon = event.getLongitude();

            int dayScore = noiseScoringService.calculateNoiseScore(lat, lon);


            AnalysisResult result = AnalysisResult.builder()
                    .houseId(event.getHouseId())
                    .latitude(lat)
                    .longitude(lon)
                    .noiseScore(dayScore)
                    .build();

            analysisResultRepository.save(result);

        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생", e);
        }
    }
}