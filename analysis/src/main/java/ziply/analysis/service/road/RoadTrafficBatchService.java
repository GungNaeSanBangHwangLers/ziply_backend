package ziply.analysis.service.road;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.analysis.domain.RoadSegmentEntity;
import ziply.analysis.domain.RoadTrafficEntity;
import ziply.analysis.dto.road.RealtimeRoadTrafficDto;
import ziply.analysis.repository.RoadSegmentRepository;
import ziply.analysis.repository.RoadTrafficRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RoadTrafficBatchService {

    private final RealtimeRoadTrafficService apiService;
    private final RoadSegmentRepository segmentRepository;
    private final RoadTrafficRepository trafficRepository;

    public RoadTrafficBatchService(RealtimeRoadTrafficService apiService,
                                   RoadSegmentRepository segmentRepository,
                                   RoadTrafficRepository trafficRepository) {
        this.apiService = apiService;
        this.segmentRepository = segmentRepository;
        this.trafficRepository = trafficRepository;
    }

    @Transactional
    public void loadInitialRoadData() {
        System.out.println("🚦 [STEP 3] 실시간 도로 소통 정보(T3/T4) 적재 시작...");

        List<RealtimeRoadTrafficDto> trafficData = apiService.fetchRealtimeTraffic();

        if (trafficData == null || trafficData.isEmpty()) {
            System.err.println("⚠️ 수집된 실시간 데이터가 0건입니다. (API 키 권한 또는 서버 상태 확인)");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int successCount = 0;

        for (RealtimeRoadTrafficDto dto : trafficData) {
            try {
                // T3: 도로 구간 기본 정보 (중복 방지)
                if (dto.getLinkId() != null && !segmentRepository.existsById(dto.getLinkId())) {
                    segmentRepository.save(RoadSegmentEntity.builder()
                            .segmentId(dto.getLinkId())
                            .roadDivNm(dto.getRoadNm())
                            .startNodeNm(dto.getStartNodeNm())
                            .endNodeNm(dto.getEndNodeNm())
                            .build());
                }

                // T4: 실시간 속도 정보 (prcSpd -> Double 변환)
                double speed = (dto.getPrcSpd() != null) ? Double.parseDouble(dto.getPrcSpd()) : 0.0;

                // 시간 코드 추출 (YYYYMMDDHHMMSS 구조에서 HH 추출)
                String timeCode = "00";
                if (dto.getCurDate() != null && dto.getCurDate().length() >= 10) {
                    timeCode = dto.getCurDate().substring(8, 10);
                }

                trafficRepository.save(RoadTrafficEntity.builder()
                        .segmentId(dto.getLinkId())
                        .avgSpd(speed)
                        .timeCode(timeCode)
                        .collectTime(now)
                        .build());

                successCount++;
            } catch (Exception e) {
                // 개별 데이터 오류 시 로그만 찍고 다음 데이터 진행
                System.err.println("⏩ 데이터 처리 건너뜀 (LinkId: " + dto.getLinkId() + "): " + e.getMessage());
            }
        }

        System.out.println("✅ 실시간 도로 데이터 총 " + successCount + "건 적재 성공!");
    }
}