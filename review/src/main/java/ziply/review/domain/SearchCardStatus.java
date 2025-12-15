package ziply.review.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SearchCardStatus {

    PLANNED("탐색 예정"),
    IN_PROGRESS("탐색중"),
    COMPLETED("탐색 완료");

    @JsonValue
    private final String description;
}