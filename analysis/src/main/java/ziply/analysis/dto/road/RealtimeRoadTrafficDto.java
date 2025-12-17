package ziply.analysis.dto.road;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RealtimeRoadTrafficDto {
    @JsonProperty("road_nm")     // 도로명
    private String roadNm;

    @JsonProperty("link_id")     // 링크 ID (T3, T4 연결고리)
    private String linkId;

    @JsonProperty("prc_spd")     // 현재 평균 속도 (실시간 데이터의 핵심)
    private String prcSpd;

    @JsonProperty("st_nm")       // 시점명
    private String startNodeNm;

    @JsonProperty("ed_nm")       // 종점명
    private String endNodeNm;

    @JsonProperty("monday")      // 기준일시 (YYYYMMDDHHMMSS)
    private String curDate;
}