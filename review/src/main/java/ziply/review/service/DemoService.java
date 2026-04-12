package ziply.review.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.review.domain.House;
import ziply.review.domain.SearchCard;
import ziply.review.dto.response.DemoResetResponse;
import ziply.review.event.DemoDataResetEvent;
import ziply.review.repository.HouseImageRepository;
import ziply.review.repository.HouseRepository;
import ziply.review.repository.MeasurementRepository;
import ziply.review.repository.SearchCardRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoService {

    private final SearchCardRepository searchCardRepository;
    private final HouseRepository houseRepository;
    private final MeasurementRepository measurementRepository;
    private final HouseImageRepository houseImageRepository;
    private final ReviewProducerService reviewProducerService;

    /**
     * 데모 계정의 모든 데이터 초기화
     * 
     * @param userId 사용자 ID
     * @return 삭제된 데이터 통계
     */
    @Transactional
    public DemoResetResponse resetDemoData(Long userId) {
        log.info("[DEMO] 데이터 초기화 시작 - userId: {}", userId);

        // 1. 해당 유저의 모든 SearchCard 조회
        List<SearchCard> searchCards = searchCardRepository.findAllByUserId(userId);
        
        if (searchCards.isEmpty()) {
            log.info("[DEMO] 삭제할 데이터 없음 - userId: {}", userId);
            return buildEmptyResponse();
        }

        // 2. 각 SearchCard에 속한 모든 House 조회
        List<House> allHouses = searchCards.stream()
                .flatMap(card -> houseRepository.findBySearchCardId(card.getId()).stream())
                .collect(Collectors.toList());

        List<Long> houseIds = allHouses.stream()
                .map(House::getId)
                .collect(Collectors.toList());

        // 3. 통계 수집 (삭제 전)
        int searchCardCount = searchCards.size();
        int houseCount = allHouses.size();
        int measurementCount = 0;
        int imageCount = 0;

        for (Long houseId : houseIds) {
            measurementCount += measurementRepository.countByHouseId(houseId);
            imageCount += houseImageRepository.findAllByHouseId(houseId).size();
        }

        // 4. 데이터 삭제 (Cascade로 자동 삭제되지만 명시적으로 처리)
        for (Long houseId : houseIds) {
            measurementRepository.deleteAllByHouseId(houseId);
            houseImageRepository.deleteAllByHouseId(houseId);
        }
        houseRepository.deleteAll(allHouses);
        searchCardRepository.deleteAll(searchCards);

        // 5. Kafka 이벤트 발행 (Analysis 서비스에 알림)
        DemoDataResetEvent event = new DemoDataResetEvent(userId, houseIds, System.currentTimeMillis());
        reviewProducerService.sendDemoDataResetEvent(event);

        log.info("[DEMO] 데이터 삭제 완료 - Cards: {}, Houses: {}, Measurements: {}, Images: {}", 
                searchCardCount, houseCount, measurementCount, imageCount);

        return DemoResetResponse.builder()
                .message("데모 데이터가 초기화되었습니다.")
                .deletedData(DemoResetResponse.DeletedData.builder()
                        .searchCards(searchCardCount)
                        .houses(houseCount)
                        .measurements(measurementCount)
                        .images(imageCount)
                        .build())
                .build();
    }

    private DemoResetResponse buildEmptyResponse() {
        return DemoResetResponse.builder()
                .message("삭제할 데이터가 없습니다.")
                .deletedData(DemoResetResponse.DeletedData.builder()
                        .searchCards(0)
                        .houses(0)
                        .measurements(0)
                        .images(0)
                        .build())
                .build();
    }
}
