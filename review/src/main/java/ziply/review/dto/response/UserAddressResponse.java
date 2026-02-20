package ziply.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserAddressResponse {
    private List<AddressInfo> addresses;
}