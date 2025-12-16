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
    }

    @Getter
    @Setter
    @ToString
    public static class Address {
        private String address_name;
    }
}