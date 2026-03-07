package ziply.review.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ziply.review.domain.House;
import ziply.review.domain.HouseImage;
import ziply.review.domain.HouseStatus;
import ziply.review.domain.Measurement;
import ziply.review.dto.request.DirectionRequest;
import ziply.review.dto.request.LightLevelRequest;
import ziply.review.dto.response.DirectionGroupResponse;
import ziply.review.dto.response.HouseSunlightResponse;
import ziply.review.dto.response.MeasurementCardResponse;
import ziply.review.repository.HouseImageRepository;
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
    private final HouseImageRepository houseImageRepository;
    private final DirectionMapper directionMapper;
    private final ImageUploadService imageUploadService;

    /**
     * 이미지 업로드
     */
    public void uploadImages(Long userId, Long houseId, List<MultipartFile> images) {
        House house = getHouseAndValidate(userId, houseId);
        if (images != null && !images.isEmpty()) {
            List<String> uploadedUrls = imageUploadService.uploadImages(images);
            uploadedUrls.forEach(url ->
                    houseImageRepository.save(HouseImage.builder()
                            .house(house)
                            .imageUrl(url)
                            .build())
            );
        }
    }

    /**
     * 특정 회차(Round) 측정 데이터 삭제 및 재정렬
     */
    public void deleteMeasurementByRound(Long userId, Long houseId, Integer round) {
        getHouseAndValidate(userId, houseId);

        Measurement measurement = measurementRepository.findByHouseIdAndRound(houseId, round)
                .orElseThrow(() -> new IllegalArgumentException(round + "차 측정 기록이 없습니다."));

        measurementRepository.delete(measurement);
        measurementRepository.reorderRounds(houseId, round);

        if (measurementRepository.countByHouseId(houseId) == 0) {
            houseRepository.findById(houseId).ifPresent(h -> h.updateStatus(HouseStatus.BEFORE));
        }
    }

    /**
     * 하우스 모든 측정 데이터 삭제
     */
    public void deleteHouseMeasurement(Long userId, Long houseId) {
        getHouseAndValidate(userId, houseId);
        measurementRepository.deleteAllByHouseId(houseId);
        houseImageRepository.deleteAllByHouseId(houseId);
        houseRepository.findById(houseId).ifPresent(h -> h.updateStatus(HouseStatus.BEFORE));
    }

    // --- [조회 기능들] ---

    /**
     * 측정 카드 데이터 조회 (회차별 리스트)
     */
    @Transactional(readOnly = true)
    public List<MeasurementCardResponse> getMeasurementCardData(Long userId, Long houseId) {
        getHouseAndValidate(userId, houseId);
        List<Measurement> measurements = measurementRepository.findAllByHouseId(houseId);
        List<String> imageUrls = houseImageRepository.findAllByHouseId(houseId).stream()
                .map(HouseImage::getImageUrl)
                .toList();

        return measurements.stream()
                .map(m -> new MeasurementCardResponse(
                        m.getRound(),
                        m.getRound() + "차 측정",
                        m.getDirection() != null,
                        m.getLightLevel() != null,
                        m.getDirection() != null ? "방향 측정 완료" : "데이터 없음",
                        m.getLightLevel() != null ? "채광 측정 완료" : "데이터 없음",
                        m.getDirection(),
                        m.getLightLevel(),
                        imageUrls
                ))
                .collect(Collectors.toList());
    }

    /**
     * 탐색 카드별 평균 채광 점수 조회
     */
    @Transactional(readOnly = true)
    public List<HouseSunlightResponse> getHouseSunlightScoresByCard(UUID cardId, Long userId) {
        List<Measurement> allMeasurements = measurementRepository.findAllBySearchCardId(cardId);
        if (allMeasurements.isEmpty()) return List.of();

        Map<Long, List<Measurement>> grouped = allMeasurements.stream()
                .collect(Collectors.groupingBy(m -> m.getHouse().getId()));

        List<Long> houseIds = grouped.keySet().stream().sorted().toList();

        return IntStream.range(0, houseIds.size()).mapToObj(i -> {
            Long houseId = houseIds.get(i);
            double avg = grouped.get(houseId).stream()
                    .map(Measurement::getLightLevel)
                    .filter(l -> l != null)
                    .mapToDouble(Double::doubleValue)
                    .average().orElse(0.0);
            return new HouseSunlightResponse(houseId, String.valueOf((char)('A' + i)), (int)Math.min(Math.round((avg/2500.0)*100), 100));
        }).toList();
    }

    /**
     * 탐색 카드 전체의 향 정보 그룹화 조회 (A~G 별칭 포함)
     */
    @Transactional(readOnly = true)
    public List<DirectionGroupResponse> getDirectionGroups(Long userId, UUID searchCardId) {
        searchCardRepository.findById(searchCardId)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("권한 없음"));

        List<Measurement> allMeasurements = measurementRepository.findAllBySearchCardId(searchCardId);
        return convertToDirectionGroupResponses(allMeasurements);
    }

    /**
     * 특정 하우스의 향 정보 조회 (해당 카드의 일관된 별칭 유지)
     */
    @Transactional(readOnly = true)
    public List<DirectionGroupResponse> getDirectionGroupsByHouse(Long userId, Long houseId) {
        House house = getHouseAndValidate(userId, houseId);
        List<Measurement> allMeasurements = measurementRepository.findAllBySearchCardId(house.getSearchCard().getId());

        return convertToDirectionGroupResponses(allMeasurements).stream()
                .filter(res -> res.houseId().equals(houseId))
                .toList();
    }

    /**
     * 하우스별 별칭(A-G) 부여 및 창문 데이터 변환 공통 로직
     */
    private List<DirectionGroupResponse> convertToDirectionGroupResponses(List<Measurement> allMeasurements) {
        if (allMeasurements.isEmpty()) return List.of();

        // 1. 하우스 ID 순 정렬 후 별칭(A~G) 매핑
        List<Long> sortedHouseIds = allMeasurements.stream()
                .map(m -> m.getHouse().getId())
                .distinct()
                .sorted()
                .limit(7)
                .toList();

        Map<Long, String> houseAliasMap = IntStream.range(0, sortedHouseIds.size())
                .boxed()
                .collect(Collectors.toMap(
                        sortedHouseIds::get,
                        i -> String.valueOf((char) ('A' + i))
                ));

        // 2. 하우스별 그룹화
        Map<Long, List<Measurement>> groupedByHouse = allMeasurements.stream()
                .collect(Collectors.groupingBy(m -> m.getHouse().getId()));

        // 3. DTO 변환
        return sortedHouseIds.stream()
                .map(houseId -> {
                    List<DirectionGroupResponse.WindowDirectionDetail> windows = groupedByHouse.get(houseId).stream()
                            .filter(m -> m.getDirectionType() != null)
                            .map(m -> new DirectionGroupResponse.WindowDirectionDetail(
                                    m.getWindowLocation(),
                                    m.getDirectionType(),
                                    m.getDirectionFeatures(),
                                    m.getDirectionPros(),
                                    m.getDirectionCons()
                            ))
                            .toList();

                    return new DirectionGroupResponse(houseAliasMap.get(houseId), houseId, windows);
                }).toList();
    }

    // --- [저장 기능들] ---

    public void saveDirection(Long userId, Long houseId, DirectionRequest request) {
        House house = getHouseAndValidate(userId, houseId);
        DirectionMapper.DirectionInfo info = directionMapper.getInfo(request.direction());

        Measurement measurement = measurementRepository.findByHouseIdAndRound(houseId, request.round())
                .orElseGet(() -> createEmptyMeasurement(house, request.round()));

        measurement.updateDirection(
                request.direction(),
                info.type(),
                info.features(),
                info.pros(),
                info.cons(),
                request.windowLocation()
        );

        measurementRepository.save(measurement);
        house.updateStatus(HouseStatus.AFTER);
    }

    public void saveLightLevel(Long userId, Long houseId, LightLevelRequest request) {
        House house = getHouseAndValidate(userId, houseId);
        Double representativeLux = calculateRepresentativeLux(request.lightLevels());

        Measurement measurement = measurementRepository.findByHouseIdAndRound(houseId, request.round())
                .orElseGet(() -> createEmptyMeasurement(house, request.round()));

        measurement.updateLightLevel(representativeLux);

        measurementRepository.save(measurement);
        house.updateStatus(HouseStatus.AFTER);
    }

    // --- [내부 유틸] ---

    private House getHouseAndValidate(Long userId, Long houseId) {
        House house = houseRepository.findById(houseId).orElseThrow(() -> new IllegalArgumentException("하우스 없음"));
        if (!house.getSearchCard().getUserId().equals(userId)) throw new IllegalStateException("권한 없음");
        return house;
    }

    private Double calculateRepresentativeLux(List<Double> rawLuxValues) {
        if (rawLuxValues == null || rawLuxValues.isEmpty()) return null;
        List<Double> valid = new ArrayList<>();
        for (int i = 0; i < rawLuxValues.size(); i++) {
            double cur = rawLuxValues.get(i);
            if (i == 0) { valid.add(cur); continue; }
            if (!(cur >= rawLuxValues.get(i-1)*2) && !(Math.abs(cur - rawLuxValues.get(i-1)) >= 300)) valid.add(cur);
        }
        if (valid.size() >= 3) { Collections.sort(valid); return valid.get(valid.size()/2); }
        return (valid.size() > 0) ? valid.get(0) : null;
    }

    private Measurement createEmptyMeasurement(House house, Integer round) {
        return Measurement.builder()
                .house(house)
                .round(round)
                .build();
    }
}