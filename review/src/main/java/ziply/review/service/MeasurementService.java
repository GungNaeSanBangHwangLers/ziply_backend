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
import ziply.review.domain.SearchCard;
import ziply.review.dto.request.CombinedMeasurementRequest;
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
     * 1. 통합 측정 데이터 저장 (방향 + 채광 + 라운드 자동화)
     */
    public void saveCombinedMeasurement(Long userId, Long houseId, CombinedMeasurementRequest request) {
        House house = getHouseAndValidate(userId, houseId);

        // 현재 저장된 측정 개수 기반으로 다음 라운드(1, 2, 3...) 지정
        int nextRound = measurementRepository.countByHouseId(houseId) + 1;

        Double representativeLux = calculateRepresentativeLux(request.lightLevels());
        DirectionMapper.DirectionInfo info = directionMapper.getInfo(request.direction());

        Measurement measurement = Measurement.builder()
                .house(house)
                .round(nextRound)
                .direction(request.direction())
                .directionType(info.type())
                .directionFeatures(info.features())
                .directionPros(info.pros())
                .directionCons(info.cons())
                .lightLevel(representativeLux)
                .windowLocation(request.windowLocation())
                .build();

        measurementRepository.save(measurement);
        house.updateStatus(HouseStatus.AFTER);
    }

    /**
     * 2. 이미지 업로드
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
     * 3. 특정 라운드 삭제 및 재정렬
     */
    public void deleteMeasurementByRound(Long userId, Long houseId, Integer round) {
        getHouseAndValidate(userId, houseId);

        Measurement measurement = measurementRepository.findByHouseIdAndRound(houseId, round)
                .orElseThrow(() -> new IllegalArgumentException(round + "차 측정 기록이 없습니다."));

        // 삭제
        measurementRepository.delete(measurement);

        // 뒷번호 당기기 (재정렬)
        measurementRepository.reorderRounds(houseId, round);

        // 데이터가 하나도 없으면 상태 복구
        if (measurementRepository.countByHouseId(houseId) == 0) {
            houseRepository.findById(houseId).ifPresent(h -> h.updateStatus(HouseStatus.BEFORE));
        }
    }

    /**
     * 4. 하우스 측정 전체 삭제
     */
    public void deleteHouseMeasurement(Long userId, Long houseId) {
        getHouseAndValidate(userId, houseId);
        measurementRepository.deleteAllByHouseId(houseId);
        houseImageRepository.deleteAllByHouseId(houseId);
        houseRepository.findById(houseId).ifPresent(h -> h.updateStatus(HouseStatus.BEFORE));
    }

    // --- [조회 기능들] ---

    @Transactional(readOnly = true)
    public List<MeasurementCardResponse> getMeasurementCardData(Long userId, Long houseId) {
        House house = getHouseAndValidate(userId, houseId);

        List<Measurement> measurements = measurementRepository.findAllByHouseId(houseId);

        // 해당 하우스의 모든 이미지 URL 리스트 추출
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

    @Transactional(readOnly = true)
    public List<HouseSunlightResponse> getHouseSunlightScoresByCard(UUID cardId, Long userId) {
        List<Measurement> allMeasurements = measurementRepository.findAllBySearchCardId(cardId);
        if (allMeasurements.isEmpty()) return List.of();
        Map<Long, List<Measurement>> grouped = allMeasurements.stream().collect(Collectors.groupingBy(m -> m.getHouse().getId()));
        List<Long> houseIds = grouped.keySet().stream().sorted().toList();
        return IntStream.range(0, houseIds.size()).mapToObj(i -> {
            Long houseId = houseIds.get(i);
            double avg = grouped.get(houseId).stream().map(Measurement::getLightLevel).filter(l -> l != null).mapToDouble(Double::doubleValue).average().orElse(0.0);
            return new HouseSunlightResponse(houseId, String.valueOf((char)('A' + i)), (int)Math.min(Math.round((avg/2500.0)*100), 100));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<DirectionGroupResponse> getDirectionGroups(Long userId, UUID searchCardId) {
        searchCardRepository.findById(searchCardId).filter(c -> c.getUserId().equals(userId)).orElseThrow(() -> new IllegalArgumentException("권한 없음"));
        return processDirectionGroups(measurementRepository.findAllBySearchCardId(searchCardId), null);
    }

    @Transactional(readOnly = true)
    public List<DirectionGroupResponse> getDirectionGroupsByHouse(Long userId, Long houseId) {
        getHouseAndValidate(userId, houseId);
        return processDirectionGroups(measurementRepository.findAllByHouseId(houseId), houseId);
    }

    // --- [내부 유틸] ---

    private House getHouseAndValidate(Long userId, Long houseId) {
        House house = houseRepository.findById(houseId).orElseThrow(() -> new IllegalArgumentException("하우스 없음"));
        if (!house.getSearchCard().getUserId().equals(userId)) throw new IllegalStateException("권한 없음");
        return house;
    }

    private List<DirectionGroupResponse> processDirectionGroups(List<Measurement> measurements, Long houseId) {
        Map<String, List<Measurement>> grouped = measurements.stream().filter(m -> m.getDirectionType() != null).collect(Collectors.groupingBy(Measurement::getDirectionType));
        return grouped.entrySet().stream().map(e -> {
            Measurement s = e.getValue().get(0);
            List<Long> ids = houseId != null ? List.of(houseId) : e.getValue().stream().map(m -> m.getHouse().getId()).distinct().toList();
            return new DirectionGroupResponse(e.getKey(), s.getDirectionFeatures(), s.getDirectionPros(), s.getDirectionCons(), ids);
        }).toList();
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
        return null;
    }
}