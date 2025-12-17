package ziply.analysis.service.road;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@RequiredArgsConstructor
public class RoadTrafficDataInitializer {

    private final RoadTrafficBatchService roadTrafficBatchService;

    @Bean
    @Order(3)
    public CommandLineRunner initRoadTrafficData() {
        return args -> {
            try {
                roadTrafficBatchService.loadInitialRoadData();
            } catch (Exception e) {
                System.err.println("❌ 도로 교통 데이터 초기 적재 중 오류 발생: " + e.getMessage());
            }
        };
    }
}