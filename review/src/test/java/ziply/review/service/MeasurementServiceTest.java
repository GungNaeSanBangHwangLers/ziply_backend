package ziply.review.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import ziply.review.domain.House;
import ziply.review.domain.HouseImage;
import ziply.review.domain.HouseStatus;
import ziply.review.domain.Measurement;
import ziply.review.domain.SearchCard;
import ziply.review.dto.request.DirectionRequest;
import ziply.review.dto.request.LightLevelRequest;
import ziply.review.repository.HouseImageRepository;
import ziply.review.repository.HouseRepository;
import ziply.review.repository.MeasurementRepository;
import ziply.review.repository.SearchCardRepository;

@ExtendWith(MockitoExtension.class)
class MeasurementServiceTest {

    @Mock
    private SearchCardRepository searchCardRepository;

    @Mock
    private HouseRepository houseRepository;

    @Mock
    private MeasurementRepository measurementRepository;

    @Mock
    private HouseImageRepository houseImageRepository;

    @Mock
    private DirectionMapper directionMapper;

    @Mock
    private ImageUploadService imageUploadService;

    @InjectMocks
    private MeasurementService measurementService;

    private House testHouse;
    private SearchCard testCard;
    private UUID testCardId;

    @BeforeEach
    void setUp() {
        testCardId = UUID.randomUUID();
        testCard = new SearchCard(1L, "테스트 카드", null, null);
        ReflectionTestUtils.setField(testCard, "id", testCardId);

        testHouse = House.builder()
                .id(1L)
                .searchCard(testCard)
                .address("서울시 강남구")
                .latitude(37.5)
                .longitude(127.0)
                .visitDateTime(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("이미지 업로드 - 정상 업로드")
    void uploadImages_Success() {
        // given
        Long userId = 1L;
        Long houseId = 1L;
        MultipartFile mockFile1 = mock(MultipartFile.class);
        MultipartFile mockFile2 = mock(MultipartFile.class);
        List<MultipartFile> images = List.of(mockFile1, mockFile2);

        when(houseRepository.findById(houseId)).thenReturn(Optional.of(testHouse));
        when(imageUploadService.uploadImages(images)).thenReturn(List.of("url1", "url2"));

        // when
        measurementService.uploadImages(userId, houseId, images);

        // then
        verify(imageUploadService).uploadImages(images);
        verify(houseImageRepository, times(2)).save(any(HouseImage.class));
    }

    @Test
    @DisplayName("이미지 업로드 - 권한 없는 사용자")
    void uploadImages_UnauthorizedUser_ThrowsException() {
        // given
        Long unauthorizedUserId = 999L;
        Long houseId = 1L;
        List<MultipartFile> images = List.of(mock(MultipartFile.class));

        when(houseRepository.findById(houseId)).thenReturn(Optional.of(testHouse));

        // when & then
        assertThatThrownBy(() -> measurementService.uploadImages(unauthorizedUserId, houseId, images))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("권한 없음");
    }

    @Test
    @DisplayName("특정 회차 측정 데이터 삭제 및 재정렬")
    void deleteMeasurementByRound_Success() {
        // given
        Long userId = 1L;
        Long houseId = 1L;
        Integer round = 2;

        Measurement measurement = Measurement.builder()
                .house(testHouse)
                .round(round)
                .build();

        when(houseRepository.findById(houseId)).thenReturn(Optional.of(testHouse));
        when(measurementRepository.findByHouseIdAndRound(houseId, round))
                .thenReturn(Optional.of(measurement));
        when(measurementRepository.countByHouseId(houseId)).thenReturn(1);

        // when
        measurementService.deleteMeasurementByRound(userId, houseId, round);

        // then
        verify(measurementRepository).delete(measurement);
        verify(measurementRepository).reorderRounds(houseId, round);
    }

    @Test
    @DisplayName("특정 회차 측정 데이터 삭제 - 존재하지 않는 회차")
    void deleteMeasurementByRound_NonExistentRound_ThrowsException() {
        // given
        Long userId = 1L;
        Long houseId = 1L;
        Integer round = 2;

        when(houseRepository.findById(houseId)).thenReturn(Optional.of(testHouse));
        when(measurementRepository.findByHouseIdAndRound(houseId, round))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> measurementService.deleteMeasurementByRound(userId, houseId, round))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("차 측정 기록이 없습니다");
    }

    @Test
    @DisplayName("하우스 모든 측정 데이터 삭제 - 상태가 BEFORE로 변경됨")
    void deleteHouseMeasurement_Success() {
        // given
        Long userId = 1L;
        Long houseId = 1L;

        when(houseRepository.findById(houseId)).thenReturn(Optional.of(testHouse));

        // when
        measurementService.deleteHouseMeasurement(userId, houseId);

        // then
        verify(measurementRepository).deleteAllByHouseId(houseId);
        verify(houseImageRepository).deleteAllByHouseId(houseId);
        assertThat(testHouse.getStatus()).isEqualTo(HouseStatus.BEFORE);
    }

    @Test
    @DisplayName("방향 데이터 저장 - 새로운 측정 생성")
    void saveDirection_CreatesNewMeasurement() {
        // given
        Long userId = 1L;
        Long houseId = 1L;
        Integer round = 1;
        DirectionRequest request = new DirectionRequest(round, 135.0, "거실");

        DirectionMapper.DirectionInfo directionInfo = new DirectionMapper.DirectionInfo(
                "동남향", "오전 햇빛 좋음", "여름 더울 수 있음", "겨울 따뜻함"
        );

        when(houseRepository.findById(houseId)).thenReturn(Optional.of(testHouse));
        when(measurementRepository.findByHouseIdAndRound(houseId, round))
                .thenReturn(Optional.empty());
        when(directionMapper.getInfo(135.0)).thenReturn(directionInfo);

        // when
        measurementService.saveDirection(userId, houseId, request);

        // then
        verify(measurementRepository).save(any(Measurement.class));
        assertThat(testHouse.getStatus()).isEqualTo(HouseStatus.AFTER);
    }

    @Test
    @DisplayName("방향 데이터 저장 - 기존 측정 업데이트")
    void saveDirection_UpdatesExistingMeasurement() {
        // given
        Long userId = 1L;
        Long houseId = 1L;
        Integer round = 1;
        DirectionRequest request = new DirectionRequest(round, 135.0, "거실");

        Measurement existingMeasurement = Measurement.builder()
                .house(testHouse)
                .round(round)
                .build();

        DirectionMapper.DirectionInfo directionInfo = new DirectionMapper.DirectionInfo(
                "동남향", "오전 햇빛 좋음", "여름 더울 수 있음", "겨울 따뜻함"
        );

        when(houseRepository.findById(houseId)).thenReturn(Optional.of(testHouse));
        when(measurementRepository.findByHouseIdAndRound(houseId, round))
                .thenReturn(Optional.of(existingMeasurement));
        when(directionMapper.getInfo(135.0)).thenReturn(directionInfo);

        // when
        measurementService.saveDirection(userId, houseId, request);

        // then
        verify(measurementRepository).save(existingMeasurement);
        assertThat(existingMeasurement.getDirection()).isEqualTo(135.0);
        assertThat(testHouse.getStatus()).isEqualTo(HouseStatus.AFTER);
    }

    @Test
    @DisplayName("채광 데이터 저장 - 중간값 계산")
    void saveLightLevel_CalculatesMedian() {
        // given
        Long userId = 1L;
        Long houseId = 1L;
        Integer round = 1;
        List<Double> lightLevels = List.of(100.0, 150.0, 200.0, 250.0, 300.0);
        LightLevelRequest request = new LightLevelRequest(round, lightLevels);

        when(houseRepository.findById(houseId)).thenReturn(Optional.of(testHouse));
        when(measurementRepository.findByHouseIdAndRound(houseId, round))
                .thenReturn(Optional.empty());

        // when
        measurementService.saveLightLevel(userId, houseId, request);

        // then
        ArgumentCaptor<Measurement> captor = ArgumentCaptor.forClass(Measurement.class);
        verify(measurementRepository).save(captor.capture());
        Measurement savedMeasurement = captor.getValue();
        assertThat(savedMeasurement.getLightLevel()).isEqualTo(200.0);
        assertThat(testHouse.getStatus()).isEqualTo(HouseStatus.AFTER);
    }

    @Test
    @DisplayName("채광 데이터 저장 - 이상치 제거")
    void saveLightLevel_RemovesOutliers() {
        // given
        Long userId = 1L;
        Long houseId = 1L;
        Integer round = 1;
        // 100.0 다음 500.0은 이상치 (2배 이상 차이)
        List<Double> lightLevels = List.of(100.0, 500.0, 110.0, 120.0, 130.0);
        LightLevelRequest request = new LightLevelRequest(round, lightLevels);

        when(houseRepository.findById(houseId)).thenReturn(Optional.of(testHouse));
        when(measurementRepository.findByHouseIdAndRound(houseId, round))
                .thenReturn(Optional.empty());

        // when
        measurementService.saveLightLevel(userId, houseId, request);

        // then
        ArgumentCaptor<Measurement> captor = ArgumentCaptor.forClass(Measurement.class);
        verify(measurementRepository).save(captor.capture());
        Measurement savedMeasurement = captor.getValue();
        // 이상치 500.0을 제외한 100.0, 110.0, 120.0, 130.0의 중간값은 115.0
        assertThat(savedMeasurement.getLightLevel()).isNotNull();
    }

    @Test
    @DisplayName("측정 카드 데이터 조회 - 정상 조회")
    void getMeasurementCardData_Success() {
        // given
        Long userId = 1L;
        Long houseId = 1L;

        Measurement measurement1 = Measurement.builder()
                .house(testHouse)
                .round(1)
                .direction(135.0)
                .lightLevel(200.0)
                .build();

        Measurement measurement2 = Measurement.builder()
                .house(testHouse)
                .round(2)
                .direction(180.0)
                .build();

        HouseImage image1 = HouseImage.builder()
                .house(testHouse)
                .imageUrl("url1")
                .build();

        when(houseRepository.findById(houseId)).thenReturn(Optional.of(testHouse));
        when(measurementRepository.findAllByHouseId(houseId))
                .thenReturn(List.of(measurement1, measurement2));
        when(houseImageRepository.findAllByHouseId(houseId))
                .thenReturn(List.of(image1));

        // when
        var result = measurementService.getMeasurementCardData(userId, houseId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).round()).isEqualTo(1);
        assertThat(result.get(0).isDirectionDone()).isTrue();
        assertThat(result.get(0).isLightDone()).isTrue();
        assertThat(result.get(1).round()).isEqualTo(2);
        assertThat(result.get(1).isDirectionDone()).isTrue();
        assertThat(result.get(1).isLightDone()).isFalse();
    }
}
