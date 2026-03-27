package ziply.review.dto.response;

import ziply.review.domain.BasePoint;

public record BasePointAddressResponse(
        String alias,
        String address
) {
    public static BasePointAddressResponse from(BasePoint basePoint) {
        return new BasePointAddressResponse(
                basePoint.getAlias(),
                basePoint.getAddress()
        );
    }
}