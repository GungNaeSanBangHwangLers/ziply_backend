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
    private final DirectionMapper directionMapper;

    @Transactional
    public void addBulkMeasurements(Long userId, Long houseId, List<MeasurementRequest> requests) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("해당 하우스가 없습니다."));

        if (!house.getSearchCard().getUserId().equals(userId)) {
            throw new IllegalStateException("권한이 없습니다.");
        }

        if (requests.size() > 3) {
            throw new IllegalArgumentException("측정 데이터는 최대 3개까지만 등록할 수 있습니다.");
        }

        for (int i = 0; i < requests.size(); i++) {
            int currentRound = i + 1;
            MeasurementRequest req = requests.get(i);

            boolean exists = measurementRepository.existsByHouseIdAndRound(houseId, currentRound);

            if (!exists) {
                DirectionMapper.DirectionInfo info = directionMapper.getInfo(req.direction());

                Measurement measurement = Measurement.builder()
                        .house(house)
                        .round(currentRound)
                        .direction(req.direction())
                        .lightLevel(req.lightLevel())
                        .directionType(info.type())
                        .directionFeatures(info.features())
                        .directionPros(info.pros())
                        .directionCons(info.cons())
                        .build();

                measurementRepository.save(measurement);
            }
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

        if (!house.getSearchCard().getUserId().equals(userId)) {
            throw new IllegalStateException("권한이 없습니다.");
        }

        List<Measurement> existingMeasurements = house.getMeasurements();

        for (int i = 0; i < requests.size(); i++) {
            int currentRound = i + 1;
            MeasurementRequest req = requests.get(i);

            DirectionMapper.DirectionInfo info = directionMapper.getInfo(req.direction());

            Optional<Measurement> measurementOpt = existingMeasurements.stream()
                    .filter(m -> m.getRound().equals(currentRound))
                    .findFirst();

            if (measurementOpt.isPresent()) {
                Measurement m = measurementOpt.get();
                m.updateLightLevel(req.lightLevel());
                m.updateDirectionInfo(req.direction(), info.type(), info.features(), info.pros(), info.cons());
            } else {
                Measurement newMeasurement = Measurement.builder()
                        .house(house)
                        .round(currentRound)
                        .direction(req.direction())
                        .lightLevel(req.lightLevel())
                        .directionType(info.type())
                        .directionFeatures(info.features())
                        .directionPros(info.pros())
                        .directionCons(info.cons())
                        .build();
                measurementRepository.save(newMeasurement);
            }
        }

        house.updateStatus(HouseStatus.AFTER);
    }

    @Transactional
    public void deleteMeasurement(Long userId, Long houseId, Integer round) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("해당 하우스가 없습니다."));

        if (!house.getSearchCard().getUserId().equals(userId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        Measurement target = measurementRepository.findByHouseIdAndRound(houseId, round)
                .orElseThrow(() -> new IllegalArgumentException(round + "차 측정 데이터가 존재하지 않습니다."));

        measurementRepository.delete(target);

        if (measurementRepository.findAllByHouseIdOrderByRoundAsc(houseId).isEmpty()) {
            house.updateStatus(HouseStatus.BEFORE);
        }
    }
}