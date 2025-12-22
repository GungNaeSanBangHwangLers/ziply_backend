package ziply.review.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ziply.review.domain.SearchCard;
import ziply.review.domain.House;
import ziply.review.repository.SearchCardRepository;
import ziply.review.repository.HouseRepository;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewStatusScheduler {

    private final SearchCardRepository searchCardRepository;
    private final HouseRepository houseRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void updateAllStatuses() {
        log.info("시작: 전체 리뷰 시스템(SearchCard & House) 상태 업데이트 스케줄러 실행");

        LocalDate today = LocalDate.now();

        List<SearchCard> allCards = searchCardRepository.findAll();
        for (SearchCard card : allCards) {
            var oldStatus = card.getStatus();
            card.syncStatus(today);
            var newStatus = card.getStatus();
            if (oldStatus != newStatus) {
                log.info("[SearchCard] ID: {} 상태 변경: {} -> {}", card.getId(), oldStatus, newStatus);
            }
        }

        List<House> allHouses = houseRepository.findAll();
        for (House house : allHouses) {
            var oldStatus = house.getStatus();
            house.syncStatus(today);
            var newStatus = house.getStatus();

            if (oldStatus != newStatus) {
                log.info("[House] ID: {} 상태 변경: {} -> {}", house.getId(), oldStatus, newStatus);
            }
        }
        log.info("종료: 상태 업데이트 완료");
    }
}