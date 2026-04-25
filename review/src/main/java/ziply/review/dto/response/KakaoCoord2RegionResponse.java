package ziply.review.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class KakaoCoord2RegionResponse {

    private List<Document> documents;

    @Getter
    @Setter
    public static class Document {
        private String region_type;           // "B" = 법정동, "H" = 행정동
        private String region_1depth_name;    // 시/도 (예: 서울특별시)
        private String region_2depth_name;    // 구 (예: 동작구)
        private String region_3depth_name;    // 동 (예: 상도동)
    }
}
