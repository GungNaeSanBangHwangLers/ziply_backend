package ziply.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DemoResetResponse {
    private String message;
    private DeletedData deletedData;
    
    @Getter
    @Builder
    @AllArgsConstructor
    public static class DeletedData {
        private int searchCards;
        private int houses;
        private int measurements;
        private int images;
    }
}
