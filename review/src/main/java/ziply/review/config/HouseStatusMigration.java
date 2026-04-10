package ziply.review.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ziply.review.domain.House;
import ziply.review.domain.HouseStatus;
import ziply.review.repository.HouseRepository;

import java.util.List;

/**
 * 애플리케이션 시작 시 House 상태 검증 및 수정
 * - AFTER 상태인데 실제로 방향/채광/사진이 모두 없는 집들을 BEFORE로 되돌림
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HouseStatusMigration implements CommandLineRunner {

    private final HouseRepository houseRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("[마이그레이션] House 상태 검증 시작");

        List<House> afterHouses = houseRepository.findAll().stream()
                .filter(house -> HouseStatus.AFTER.equals(house.getStatus()))
                .toList();

        int correctedCount = 0;

        for (House house : afterHouses) {
            boolean hasDirection = house.getMeasurements().stream()
                    .anyMatch(m -> m.getDirection() != null);

            boolean hasLightLevel = house.getMeasurements().stream()
                    .anyMatch(m -> m.getLightLevel() != null);

            boolean hasImages = !house.getHouseImages().isEmpty();

            // 조건을 만족하지 않으면 BEFORE로 변경
            if (!(hasDirection && hasLightLevel && hasImages)) {
                house.updateStatus(HouseStatus.BEFORE);
                correctedCount++;
                log.debug("[마이그레이션] House ID {} 상태를 BEFORE로 수정 (방향: {}, 채광: {}, 사진: {})",
                        house.getId(), hasDirection, hasLightLevel, hasImages);
            }
        }

        if (correctedCount > 0) {
            log.info("[마이그레이션] {} 개의 House 상태를 수정했습니다.", correctedCount);
        } else {
            log.info("[마이그레이션] 수정이 필요한 House가 없습니다.");
        }
    }
}
