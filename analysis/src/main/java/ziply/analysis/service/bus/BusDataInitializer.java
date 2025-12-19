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

        if (!dataCheckService.isDataAlreadyLoaded("bus_stop_location")) {
            log.info("Loading bus stop location data...");
            locationBatchService.loadBusLocationData();
        } else {
            log.info("Bus stop location data already exists. Skipping load.");
        }

        if (!dataCheckService.isDataAlreadyLoaded("bus_operations")) {
            log.info("Loading bus operation data...");
            operationBatchService.loadBusOperationData();
        } else {
            log.info("Bus operation data already exists. Skipping load.");
        }

        log.info("Bus data initialization check completed.");
    }
}