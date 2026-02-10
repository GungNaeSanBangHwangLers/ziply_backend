package ziply.review.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.review.domain.House;
import ziply.review.domain.HouseStatus;
import ziply.review.domain.Measurement;
import ziply.review.domain.SearchCard;
import ziply.review.dto.request.MeasurementRequest;
import ziply.review.dto.response.DirectionGroupResponse;
import ziply.review.dto.response.HouseSunlightResponse;
import ziply.review.dto.response.MeasurementCardResponse;
import ziply.review.dto.response.SearchCardResponse;
import ziply.review.repository.HouseRepository;
import ziply.review.repository.MeasurementRepository;
import ziply.review.repository.SearchCardRepository;


@Service
@RequiredArgsConstructor
@Transactional
public class MeasurementService {

    private static final double MAX_LUX_STANDARD = 2500.0;
    private final SearchCardRepository searchCardRepository;
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
                Double representativeLux = calculateRepresentativeLux(req.lightLevel());

                DirectionMapper.DirectionInfo info = directionMapper.getInfo(req.direction());

                Measurement measurement = Measurement.builder().house(house).round(currentRound)
                        .direction(req.direction()).lightLevel(representativeLux).directionType(info.type())
                        .directionFeatures(info.features()).directionPros(info.pros()).directionCons(info.cons())
                        .build();

                measurementRepository.save(measurement);
            }
        }
    }

    private Double calculateRepresentativeLux(List<Double> rawLuxValues) {
        if (rawLuxValues == null || rawLuxValues.isEmpty()) {
            return null;
        }

        List<Double> validValues = new ArrayList<>();

        for (int i = 0; i < rawLuxValues.size(); i++) {
            double current = rawLuxValues.get(i);

            if (i == 0) {
                validValues.add(current);
                continue;
            }

            double prev = rawLuxValues.get(i - 1);

            boolean isDoubled = current >= prev * 2;
            boolean isSuddenChange = Math.abs(current - prev) >= 300;

            if (!isDoubled && !isSuddenChange) {
                validValues.add(current);
            }
        }

        if (validValues.size() >= 3) {
            Collections.sort(validValues);
            return validValues.get(validValues.size() / 2);
        }

        return null;
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
            Optional<Measurement> data = measurements.stream().filter(m -> m.getRound().equals(currentRound))
                    .findFirst();

            if (data.isPresent()) {
                Measurement m = data.get();
                response.add(new MeasurementCardResponse(currentRound, currentRound + "차 측정", true, true, "방향 측정 완료",
                        "채광 측정 완료", m.getDirection(), m.getLightLevel()));
            } else {
                response.add(new MeasurementCardResponse(currentRound, currentRound + "차 측정", false, false, "방향 측정 미완료",
                        "채광 측정 미완료", null, null));
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

            Double representativeLux = calculateRepresentativeLux(req.lightLevel());

            DirectionMapper.DirectionInfo info = directionMapper.getInfo(req.direction());

            Optional<Measurement> measurementOpt = existingMeasurements.stream()
                    .filter(m -> m.getRound().equals(currentRound)).findFirst();

            if (measurementOpt.isPresent()) {
                Measurement m = measurementOpt.get();
                m.updateLightLevel(representativeLux);
                m.updateDirectionInfo(req.direction(), info.type(), info.features(), info.pros(), info.cons());
            } else {
                Measurement newMeasurement = Measurement.builder().house(house).round(currentRound)
                        .direction(req.direction()).lightLevel(representativeLux).directionType(info.type())
                        .directionFeatures(info.features()).directionPros(info.pros()).directionCons(info.cons())
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

    @Transactional(readOnly = true)
    public List<DirectionGroupResponse> getDirectionGroups(Long userId, UUID searchCardId) {
        SearchCard searchCard = searchCardRepository.findById(searchCardId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주거탐색카드가 없습니다."));

        System.out.println(">>> 접속 요청 유저 ID: " + userId);
        System.out.println(">>> 카드 소유 유저 ID: " + searchCard.getUserId());

        if (!searchCard.getUserId().equals(userId)) {
            // 임시 방편: 만약 테스트를 바로 하고 싶다면 이 throw를 잠시 주석 처리하세요.
            throw new IllegalStateException("해당 카드에 대한 접근 권한이 없습니다.");
        }
        List<Measurement> measurements = measurementRepository.findAllBySearchCardId(searchCardId);

        Map<String, List<Measurement>> groupedByDirection = measurements.stream()
                .filter(m -> m.getDirectionType() != null)
                .collect(Collectors.groupingBy(Measurement::getDirectionType));

        return groupedByDirection.entrySet().stream().map(entry -> {
            String type = entry.getKey();
            List<Measurement> mList = entry.getValue();
            Measurement sample = mList.get(0);

            List<Long> houseIds = mList.stream().map(m -> m.getHouse().getId()).distinct().toList();

            return new DirectionGroupResponse(type, sample.getDirectionFeatures(), sample.getDirectionPros(),
                    sample.getDirectionCons(), houseIds);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<DirectionGroupResponse> getDirectionGroupsByHouse(Long userId, Long houseId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("해당 하우스 정보가 없습니다. ID: " + houseId));

        if (!house.getSearchCard().getUserId().equals(userId)) {
            throw new IllegalStateException("해당 하우스 정보에 대한 접근 권한이 없습니다.");
        }

        List<Measurement> measurements = measurementRepository.findAllByHouseId(houseId);

        Map<String, List<Measurement>> groupedByDirection = measurements.stream()
                .filter(m -> m.getDirectionType() != null)
                .collect(Collectors.groupingBy(Measurement::getDirectionType));

        return groupedByDirection.entrySet().stream().map(entry -> {
            String type = entry.getKey();
            List<Measurement> mList = entry.getValue();
            Measurement sample = mList.get(0);

            return new DirectionGroupResponse(type, sample.getDirectionFeatures(), sample.getDirectionPros(),
                    sample.getDirectionCons(), List.of(houseId));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<HouseSunlightResponse> getHouseSunlightScoresByCard(UUID cardId, Long userId) {
        List<Measurement> allMeasurements = measurementRepository.findAllBySearchCardId(cardId);

        if (allMeasurements.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Measurement>> groupedByHouse = allMeasurements.stream()
                .collect(Collectors.groupingBy(m -> m.getHouse().getId()));
        List<Long> houseIds = groupedByHouse.keySet().stream().sorted().toList();

        return IntStream.range(0, houseIds.size()).mapToObj(i -> {
            Long houseId = houseIds.get(i);
            List<Measurement> houseMeasurements = groupedByHouse.get(houseId);

            double averageLux = houseMeasurements.stream().map(Measurement::getLightLevel)
                    .filter(lux -> lux != null && lux > 0.0).mapToDouble(Double::doubleValue).average().orElse(0.0);

            int score = (int) Math.min(Math.round((averageLux / 2500.0) * 100), 100);
            String label = String.valueOf((char) ('A' + i));

            return new HouseSunlightResponse(houseId, label, score);
        }).toList();
    }
}