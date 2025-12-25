package ziply.analysis.service.bus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusDataInitializer implements CommandLineRunner {

    private final BusLocationBatchService locationBatchService;
    private final BusOperationBatchService operationBatchService;
    private final DataCheckService dataCheckService;

    @Override
    public void run(String... args) {
        log.info("Starting bus data initialization check...");

        // 1. 위치 데이터 적재
        if (!dataCheckService.isDataAlreadyLoaded("bus_stop_location")) {
            log.info("Loading bus stop location data...");
            locationBatchService.loadBusLocationData();
        } else {
            log.info("Bus stop location data already exists. Skipping load.");
        }

        // 2. 운행 횟수 데이터 적재
        if (!dataCheckService.isDataAlreadyLoaded("bus_operations")) {
            log.info("Loading bus operation data...");
            operationBatchService.loadBusOperationData();

            // [추가] 운행 데이터가 방금 새로 적재되었다면, 통계 테이블도 즉시 계산해서 채움
            log.info("New operation data loaded. Calculating bus stop statistics...");
            operationBatchService.updateBusStopStats();
        } else {
            log.info("Bus operation data already exists. Skipping load.");

            // [추가] 운행 데이터는 있는데 통계 데이터만 비어있는 경우를 위해 한 번 더 체크
            if (!dataCheckService.isDataAlreadyLoaded("bus_stop_stats")) {
                log.info("Bus stop statistics are missing. Calculating now...");
                operationBatchService.updateBusStopStats();
            }
        }

        log.info("Bus data initialization check completed.");
    }
}