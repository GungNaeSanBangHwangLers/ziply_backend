package ziply.review.exception;

import lombok.Getter;

import java.util.List;

/**
 * 지오코딩 실패 시 발생하는 예외
 * 어떤 주소들이 유효하지 않은지 정보를 포함
 */
@Getter
public class GeocodingFailureException extends RuntimeException {
    
    private final List<InvalidAddressInfo> invalidAddresses;
    
    public GeocodingFailureException(List<InvalidAddressInfo> invalidAddresses) {
        super(buildMessage(invalidAddresses));
        this.invalidAddresses = invalidAddresses;
    }
    
    private static String buildMessage(List<InvalidAddressInfo> invalidAddresses) {
        StringBuilder sb = new StringBuilder("유효하지 않은 주소가 있습니다: ");
        for (int i = 0; i < invalidAddresses.size(); i++) {
            InvalidAddressInfo info = invalidAddresses.get(i);
            sb.append(String.format("[%d번째] %s", info.getIndex() + 1, info.getAddress()));
            if (i < invalidAddresses.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
    
    @Getter
    public static class InvalidAddressInfo {
        private final int index;
        private final String address;
        
        public InvalidAddressInfo(int index, String address) {
            this.index = index;
            this.address = address;
        }
    }
}
