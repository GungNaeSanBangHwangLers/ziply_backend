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
import org.springframework.web.multipart.MultipartFile;
import ziply.review.domain.House;
import ziply.review.domain.HouseStatus;
import ziply.review.domain.Measurement;
import ziply.review.domain.SearchCard;
import ziply.review.dto.request.MeasurementRequest;
import ziply.review.dto.response.DirectionGroupResponse;
import ziply.review.dto.response.HouseSunlightResponse;
import ziply.review.dto.response.MeasurementCardResponse;
import ziply.review.repository.HouseRepository;
import ziply.review.repository.MeasurementRepository;
import ziply.review.repository.SearchCardRepository;


@Service
@RequiredArgsConstructor
@Transactional
public class MeasurementService {

    private final SearchCardRepository searchCardRepository;
    private final HouseRepository houseRepository;
    private final MeasurementRepository measurementRepository;
    private final DirectionMapper directionMapper;
    private final ImageUploadService imageUploadService;

    @Transactional
    public void addBulkMeasurements(Long userId, Long houseId,
                                    List<MeasurementRequest> requests,
                                    List<String> imageUrls) { // 이미지 URL 리스트 추가
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("해당 하우스가 없습니다."));

        // 1. 권한 체크
        if (!house.getSearchCard().getUserId().equals(userId)) {
            throw new IllegalStateException("권한이 없습니다.");
        }

        // 2. 비즈니스 제약 조건 체크
        if (requests.size() > 3) {
            throw new IllegalArgumentException("측정 데이터는 최대 3개까지만 등록할 수 있습니다.");
        }



        for (int i = 0; i < requests.size(); i++) {
            int currentRound = i + 1;
            MeasurementRequest req = requests.get(i);
            String currentImageUrl = imageUrls.get(i); // 현재 순서의 이미지 URL 추출

            // 3. 중복 회차 체크
            boolean exists = measurementRepository.existsByHouseIdAndRound(houseId, currentRound);

            if (!exists) {
                Double representativeLux = calculateRepresentativeLux(req.lightLevel());
                DirectionMapper.DirectionInfo info = directionMapper.getInfo(req.direction());

                // 4. 빌더에 imageUrl 추가
                Measurement measurement = Measurement.builder()
                        .house(house)
                        .round(currentRound)
                        .direction(req.direction())
                        .lightLevel(representativeLux)
                        .directionType(info.type())
                        .directionFeatures(info.features())
                        .directionPros(info.pros())
                        .directionCons(info.cons())
                        .windowLocation(req.windowLocation())
                        .imageUrl(currentImageUrl) // 업로드된 Azure URL 저장
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

        // 권한 체크
        if (!house.getSearchCard().getUserId().equals(userId)) {
            throw new IllegalStateException("권한이 없습니다.");
        }

        // DB에서 해당 하우스의 측정 데이터를 회차순으로 가져옴
        List<Measurement> measurements = measurementRepository.findAllByHouseIdOrderByRoundAsc(houseId);

        // measurements 리스트에 있는 개수만큼만 DTO로 변환하여 반환
        return measurements.stream()
                .map(m -> new MeasurementCardResponse(
                        m.getRound(),
                        m.getRound() + "차 측정",
                        true,
                        true,
                        "방향 측정 완료",
                        "채광 측정 완료",
                        m.getDirection(),
                        m.getLightLevel(),
                        m.getImageUrl()
                ))
                .toList();
    }

    @Transactional
    public void reMeasure(Long userId, Long houseId, List<MeasurementRequest> requests, List<MultipartFile> images) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("해당 하우스가 없습니다."));

        if (!house.getSearchCard().getUserId().equals(userId)) {
            throw new IllegalStateException("권한이 없습니다.");
        }

        // 1. 이미지 개수 검증 (데이터 개수와 일치해야 함)
        if (images != null && requests.size() != images.size()) {
            throw new IllegalArgumentException("요청 데이터와 이미지의 개수가 일치하지 않습니다.");
        }

        // 2. 이미지 업로드 (Azure Storage로 전송 후 URL 리스트 반환)
        List<String> imageUrls = (images != null) ? imageUploadService.uploadImages(images) : new ArrayList<>();

        List<Measurement> existingMeasurements = house.getMeasurements();

        for (int i = 0; i < requests.size(); i++) {
            int currentRound = i + 1;
            MeasurementRequest req = requests.get(i);
            Double representativeLux = calculateRepresentativeLux(req.lightLevel());
            DirectionMapper.DirectionInfo info = directionMapper.getInfo(req.direction());

            // 현재 인덱스에 맞는 이미지 URL 가져오기
            String currentImageUrl = (imageUrls.size() > i) ? imageUrls.get(i) : null;

            Optional<Measurement> measurementOpt = existingMeasurements.stream()
                    .filter(m -> m.getRound().equals(currentRound)).findFirst();

            if (measurementOpt.isPresent()) {
                // [수정] 기존 데이터 업데이트 시 이미지 URL도 함께 갱신
                Measurement m = measurementOpt.get();
                m.updateLightLevel(representativeLux);
                m.updateDirectionInfo(req.direction(), info.type(), info.features(), info.pros(), info.cons());

                if (currentImageUrl != null) {
                    m.updateImageUrl(currentImageUrl); // 엔티티에 해당 메서드 필요
                }
            } else {
                // [추가] 데이터가 없던 회차는 새로 생성
                Measurement newMeasurement = Measurement.builder()
                        .house(house)
                        .round(currentRound)
                        .direction(req.direction())
                        .lightLevel(representativeLux)
                        .directionType(info.type())
                        .directionFeatures(info.features())
                        .directionPros(info.pros())
                        .directionCons(info.cons())
                        .imageUrl(currentImageUrl) // 새 이미지 저장
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