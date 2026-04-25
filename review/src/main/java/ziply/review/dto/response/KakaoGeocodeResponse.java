package ziply.review.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.List;

@Getter
@Setter
@ToString
public class KakaoGeocodeResponse {

    private List<Document> documents;

    @Getter
    @Setter
    @ToString
    public static class Document {

        private String x;

        private String y;

        private String address_name;

        private String address_type;

        private RoadAddress road_address;

        private Address address;
    }

    @Getter
    @Setter
    @ToString
    public static class RoadAddress {
        private String address_name;
        private String region_2depth_name;   // 구 (예: 동작구)
        private String region_3depth_name;   // 법정동 (예: 상도동)
    }

    @Getter
    @Setter
    @ToString
    public static class Address {
        private String address_name;
        private String region_2depth_name;   // 구 (예: 동작구)
        private String region_3depth_name;   // 법정동 (예: 상도동)
    }
}