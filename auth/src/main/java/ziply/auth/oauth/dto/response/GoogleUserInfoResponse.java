package ziply.auth.oauth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleUserInfoResponse(
        String id,
        String email,
        @JsonProperty("verified_email") Boolean verifiedEmail,
        String name,
        String given_name,
        String family_name,
        String picture,
        String locale
) {
}
