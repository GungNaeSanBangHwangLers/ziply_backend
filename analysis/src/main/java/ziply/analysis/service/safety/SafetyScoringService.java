package ziply.analysis.service.safety;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ziply.analysis.repository.CCtvRepository;
import ziply.analysis.repository.PoliceStationRepository;
import ziply.analysis.repository.StreetLightRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class SafetyScoringService {

    private final CCtvRepository cctvRepository;
    private final StreetLightRepository streetLightRepository;
    private final PoliceStationRepository policeRepository;

    // --- [튜닝] 임계치 하향 조정 (점수를 더 쉽게 획득하도록) ---
    // 가로등: 1km 반경 180개면 충분히 안전하다고 판단 (기존 350 하향)
    private static final double MAX_STREETLIGHT = 180.0;
    // 경찰서: 반경 내 1개만 있어도 매우 안전 (기존 2.0 하향)
    private static final double MAX_POLICE = 1.0;
    // CCTV: 설치 대수 합계 400대면 최상위권 (기존 600 하향)
    private static final double MAX_CCTV = 400.0;

    // --- 가중치 재배분 ---
    private static final double WEIGHT_STREETLIGHT = 0.40;
    private static final double WEIGHT_POLICE = 0.30;
    private static final double WEIGHT_CCTV = 0.30;

    // 검색 범위: 약 1km 반경
    private static final double SEARCH_OFFSET = 0.01;

    @Getter
    @AllArgsConstructor
    public static class SafetyAnalysisResult {
        private int score;
        private int policeCount;
        private int streetlightCount;
        private int cctvCount;
    }

    public SafetyAnalysisResult analyzeSafety(double lat, double lon) {
        log.info("[DEBUG] Safety Analysis Request - Lat: {}, Lon: {}", lat, lon);

        try {
            // 1. 경찰서 조회 (500m 반경)
            int policeCount = policeRepository.countNearby(lat, lon, 500);

            // 2. CCTV 조회 (500m 반경 설치 대수 합계)
            Integer cctvQty = cctvRepository.sumQtyNearby(lat, lon, 500);
            int cctvCount = (cctvQty != null) ? cctvQty : 0;

            // 3. 가로등 조회 (약 1km 사각형 범위)
            double minLat = lat - SEARCH_OFFSET;
            double maxLat = lat + SEARCH_OFFSET;
            double minLng = lon - SEARCH_OFFSET;
            double maxLng = lon + SEARCH_OFFSET;

            int streetlightCount = streetLightRepository.countInBoundingBox(minLat, maxLat, minLng, maxLng);

            log.info("[SafetyAnalysis] Raw Data - Police: {}, CCTV: {}, Light: {}",
                    policeCount, cctvCount, streetlightCount);

            // 4. 점수 계산
            int score = calculateScore(policeCount, cctvCount, streetlightCount);

            return new SafetyAnalysisResult(score, policeCount, streetlightCount, cctvCount);

        } catch (Exception e) {
            log.error("[ERROR] Safety Analysis Failed: {}", e.getMessage());
            return new SafetyAnalysisResult(0, 0, 0, 0);
        }
    }

    private int calculateScore(int police, int cctv, int light) {
        double normalizedLight = Math.min((double) light / MAX_STREETLIGHT, 1.0);
        double normalizedPolice = Math.min((double) police / MAX_POLICE, 1.0);
        double normalizedCctv = Math.min((double) cctv / MAX_CCTV, 1.0);

        double rawScore =
                (normalizedLight * WEIGHT_STREETLIGHT) +
                        (normalizedPolice * WEIGHT_POLICE) +
                        (normalizedCctv * WEIGHT_CCTV);

        int baseScore = 55;
        int finalScore = baseScore + (int) Math.round(rawScore * 45);

        return Math.min(finalScore, 100);
    }
}