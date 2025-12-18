package ziply.analysis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.analysis.domain.HouseInfrastructure;
import ziply.analysis.repository.HouseInfrastructureRepository;

@Service
@RequiredArgsConstructor
public class KakaoInfrastructureService {
    private final KakaoInfrastructureProvider provider;
    private final HouseInfrastructureRepository infraRepository;

    @Transactional
    public void analyzeInfrastructure(Long houseId, double lat, double lon) {
        if (infraRepository.existsByHouseId(houseId)) {
            return;
        }

        int schoolCount = provider.getCategoryCount(lat, lon, "SC4", 200);
        int restaurantCount = provider.getCategoryCount(lat, lon, "FD6", 200);
        int subwayCount = provider.getCategoryCount(lat, lon, "SW8", 300);

        infraRepository.save(new HouseInfrastructure(houseId, schoolCount, restaurantCount, subwayCount));
    }
}