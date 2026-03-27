package ziply.analysis.service.safety;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import ziply.analysis.service.bus.DataCheckService;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SafetyDataInitializer implements CommandLineRunner {

    private final SafetyDataBatchService safetyBatchService;
    private final DataCheckService dataCheckService;

    @Override
    public void run(String... args) {
        log.info("Starting safety data initialization check...");

        // 1. 경찰서 데이터 적재
        if (!dataCheckService.isDataAlreadyLoaded("police_stations")) {
            log.info("Loading Police Station data...");
            String json = loadJsonFromResources("/polic.json");
            if (json != null) safetyBatchService.importPoliceStations(json);
        } else {
            log.info("Police Station data already exists. Skipping.");
        }

        // 2. 가로등 데이터 적재 (순서가 포함된 새로운 JSON 사용)
        if (!dataCheckService.isDataAlreadyLoaded("street_lights")) {
            log.info("Loading Street Light data with sequences...");
            String json = loadJsonFromResources("/light.json");
            if (json != null) safetyBatchService.importStreetLights(json);
        } else {
            log.info("Street Light data already exists. Skipping.");
        }

        // 3. CCTV 데이터 적재
        if (!dataCheckService.isDataAlreadyLoaded("cctvs")) {
            log.info("Loading CCTV data...");
            String json = loadJsonFromResources("/cctv.json");
            if (json != null) safetyBatchService.importCCtvs(json);
        } else {
            log.info("CCTV data already exists. Skipping.");
        }

        log.info("Safety data initialization check completed.");
    }

    /**
     * 리소스 폴더(src/main/resources)에서 JSON 파일을 읽어오는 헬퍼 메서드
     */
    private String loadJsonFromResources(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            InputStream inputStream = resource.getInputStream();
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to load JSON file from path: {}", path, e);
            return null;
        }
    }
}