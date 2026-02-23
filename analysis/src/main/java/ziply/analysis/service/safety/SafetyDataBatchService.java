package ziply.analysis.service.safety;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.analysis.domain.CCtv;
import ziply.analysis.domain.PoliceStation;
import ziply.analysis.domain.StreetLight;
import ziply.analysis.repository.CCtvRepository;
import ziply.analysis.repository.PoliceStationRepository;
import ziply.analysis.repository.StreetLightRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SafetyDataBatchService {

    private final PoliceStationRepository policeRepository;
    private final StreetLightRepository streetLightRepository;
    private final CCtvRepository cctvRepository;
    private final ObjectMapper objectMapper;

    // 1. 경찰서 배치 (방금 만든 243개 JSON 데이터용)
    @Transactional
    public void importPoliceStations(String jsonContent) {
        try {
            List<Map<String, Object>> data = objectMapper.readValue(jsonContent, new TypeReference<>() {});
            List<PoliceStation> stations = data.stream().map(m -> PoliceStation.builder()
                    .id(Long.valueOf(m.get("id").toString()))
                    .stationName((String) m.get("stationName"))
                    .classification((String) m.get("classification"))
                    .latitude((Double) m.get("latitude"))
                    .longitude((Double) m.get("longitude"))
                    .build()
            ).collect(Collectors.toList());
            policeRepository.saveAll(stations);
            log.info("PoliceStation 배치 완료: {}건", stations.size());
        } catch (Exception e) {
            log.error("PoliceStation 배치 실패", e);
        }
    }

    // 2. 가로등 배치 (관리번호, 위도, 경도 기반)
    @Transactional
    public void importStreetLights(String jsonContent) {
        try {
            List<Map<String, Object>> data = objectMapper.readValue(jsonContent, new TypeReference<>() {});
            List<StreetLight> lights = data.stream().map(m -> StreetLight.builder()
                    .managementNumber((String) m.get("관리번호"))
                    .latitude(Double.valueOf(m.get("위도").toString()))
                    .longitude(Double.valueOf(m.get("경도").toString()))
                    .build()
            ).collect(Collectors.toList());
            streetLightRepository.saveAll(lights);
            log.info("StreetLight 배치 완료: {}건", lights.size());
        } catch (Exception e) {
            log.error("StreetLight 배치 실패", e);
        }
    }

    // 3. CCTV 배치 (svcareaid, addr, qty, wgsypt, wgsxpt 기반)
    @Transactional
    public void importCCtvs(String jsonContent) {
        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            JsonNode dataNode = root.get("DATA");
            List<CCtv> cctvs = new ArrayList<>();
            for (JsonNode node : dataNode) {
                cctvs.add(CCtv.builder()
                        .borough(node.get("svcareaid").asText())
                        .address(node.get("addr").asText())
                        .quantity(node.get("qty").asInt())
                        .latitude(node.get("wgsypt").asDouble())
                        .longitude(node.get("wgsxpt").asDouble())
                        .build());
            }
            cctvRepository.saveAll(cctvs);
            log.info("CCtv 배치 완료: {}건", cctvs.size());
        } catch (Exception e) {
            log.error("CCtv 배치 실패", e);
        }
    }
}