package ziply.analysis.service.bus;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BusDataInitializer implements CommandLineRunner {

    private final BusLocationBatchService locationBatchService;
    private final BusOperationBatchService operationBatchService;
    private final DataCheckService dataCheckService;

    public BusDataInitializer(
            BusLocationBatchService locationBatchService,
            BusOperationBatchService operationBatchService,
            DataCheckService dataCheckService) {
        this.locationBatchService = locationBatchService;
        this.operationBatchService = operationBatchService;
        this.dataCheckService = dataCheckService;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("=================================================");

        if (!dataCheckService.isDataAlreadyLoaded("bus_stop_location")) {
            System.out.println("[STEP 1] 버스 정류장 위치 정보 적재 시작...");
            locationBatchService.loadBusLocationData();
            System.out.println("[STEP 1] 버스 정류장 위치 정보 적재 완료.");
        } else {
            System.out.println("ℹ[STEP 1] bus_stop_location 데이터가 이미 존재하여 적재를 건너뜁니다.");
        }

        System.out.println("-------------------------------------------------");

        // 2. 버스 운행 횟수 정보 적재 (테이블: bus_operations)
        if (!dataCheckService.isDataAlreadyLoaded("bus_operations")) {
            System.out.println("[STEP 2] 버스 운행 횟수 정보 적재 시작...");
            operationBatchService.loadBusOperationData();
            System.out.println("[STEP 2] 버스 운행 횟수 정보 적재 완료.");
        } else {
            System.out.println("ℹ[STEP 2] bus_operations 데이터가 이미 존재하여 적재를 건너뜁니다.");
        }

        System.out.println("=================================================");
    }
}