package ziply.analysis.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.analysis.repository.HouseRouteAnalysisRepository;
import ziply.analysis.repository.HouseInfrastructureRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoAnalysisService {

    private final HouseRouteAnalysisRepository houseRouteAnalysisRepository;
    private final HouseInfrastructureRepository houseInfrastructureRepository;

    /**
     * 데모 유저의 분석 데이터 초기화
     * Review에서 house 삭제 시 houseId로 연관 데이터 삭제
     * 
     * @param houseIds 삭제할 house ID 리스트
     */
    @Transactional
    public void resetDemoAnalysisData(List<Long> houseIds) {
        log.info("[DEMO-Analysis] 분석 데이터 초기화 시작 - houseIds: {}", houseIds);

        if (houseIds != null && !houseIds.isEmpty()) {
            for (Long houseId : houseIds) {
                houseRouteAnalysisRepository.deleteByHouseId(houseId);
                houseInfrastructureRepository.deleteByHouseId(houseId);
            }
            log.info("[DEMO-Analysis] {} 개 house의 분석 데이터 삭제 완료", houseIds.size());
        }
        
        log.info("[DEMO-Analysis] 분석 데이터 초기화 완료");
    }
}
