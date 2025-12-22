package ziply.review.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HouseStatus {

    BEFORE("탐색전"),
    IN_PROGRESS("탐색중"),
    AFTER("탐색후");

    @JsonValue
    private final String description;
}