package ziply.review.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.review.domain.House;
import ziply.review.domain.HouseStatus;
import ziply.review.domain.Measurement;
import ziply.review.dto.request.MeasurementRequest;
import ziply.review.dto.response.MeasurementCardResponse;
import ziply.review.repository.HouseRepository;
import ziply.review.repository.MeasurementRepository;


@Service
@RequiredArgsConstructor
@Transactional
public class MeasurementService {

    private final HouseRepository houseRepository;
    private final MeasurementRepository measurementRepository;

    public List<Measurement> findAllByHouseIdOrderByRoundAsc(Long userId, Long houseId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("해당 하우스가 없습니다."));

        if (!house.getSearchCard().getUserId().equals(userId)) {
            throw new IllegalStateException("해당 하우스에 대한 접근 권한이 없습니다.");
        }

        return measurementRepository.findAllByHouseIdOrderByRoundAsc(houseId);
    }

    @Transactional
    public void addBulkMeasurements(Long userId, Long houseId, List<MeasurementRequest> requests) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("해당 하우스가 없습니다."));

        if (!house.getSearchCard().getUserId().equals(userId)) {
            throw new IllegalStateException("권한이 없습니다.");
        }

        for (int i = 0; i < requests.size(); i++) {
            MeasurementRequest req = requests.get(i);
            Measurement measurement = Measurement.builder()
                    .house(house)
                    .round(i + 1)
                    .direction(req.direction())
                    .lightLevel(req.lightLevel())
                    .build();

            measurementRepository.save(measurement);
        }

        house.updateStatus(HouseStatus.AFTER);
    }

    public List<MeasurementCardResponse> getMeasurementCardData(Long userId, Long houseId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("해당 하우스가 없습니다."));

        if (!house.getSearchCard().getUserId().equals(userId)) {
            throw new IllegalStateException("권한이 없습니다.");
        }

        List<Measurement> measurements = measurementRepository.findAllByHouseIdOrderByRoundAsc(houseId);

        List<MeasurementCardResponse> response = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            int currentRound = i;
            Optional<Measurement> data = measurements.stream()
                    .filter(m -> m.getRound().equals(currentRound))
                    .findFirst();

            if (data.isPresent()) {
                Measurement m = data.get();
                response.add(new MeasurementCardResponse(
                        currentRound,
                        currentRound + "차 측정",
                        true, true,
                        "방향 측정 완료", "채광 측정 완료",
                        m.getDirection(), m.getLightLevel()
                ));
            } else {
                response.add(new MeasurementCardResponse(
                        currentRound,
                        currentRound + "차 측정",
                        false, false,
                        "방향 측정 미완료", "채광 측정 미완료",
                        null, null
                ));
            }
        }
        return response;
    }

    @Transactional
    public void reMeasure(Long userId, Long houseId, List<MeasurementRequest> requests) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("해당 하우스가 없습니다."));

        System.out.println("로그인한 유저 ID (토큰): " + userId);
        System.out.println("하우스 주인 ID (DB): " + house.getSearchCard().getUserId());

        if (!house.getSearchCard().getUserId().equals(userId)) {
            throw new IllegalStateException("권한이 없습니다.");
        }

        List<Measurement> existingMeasurements = house.getMeasurements();

        for (int i = 0; i < requests.size(); i++) {
            int currentRound = i + 1;
            MeasurementRequest req = requests.get(i);

            Optional<Measurement> measurementOpt = existingMeasurements.stream()
                    .filter(m -> m.getRound() == currentRound)
                    .findFirst();

            if (measurementOpt.isPresent()) {
                Measurement m = measurementOpt.get();
                m.updateDirection(req.direction());
                m.updateLightLevel(req.lightLevel());
            } else {
                Measurement newMeasurement = Measurement.builder()
                        .house(house)
                        .round(currentRound)
                        .direction(req.direction())
                        .lightLevel(req.lightLevel())
                        .build();
                measurementRepository.save(newMeasurement);
            }
        }

        house.updateStatus(HouseStatus.AFTER);
    }
}