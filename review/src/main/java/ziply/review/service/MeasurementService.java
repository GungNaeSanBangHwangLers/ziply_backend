package ziply.review.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.review.domain.House;
import ziply.review.domain.HouseStatus;
import ziply.review.domain.Measurement;
import ziply.review.dto.request.MeasurementRequest;
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
}