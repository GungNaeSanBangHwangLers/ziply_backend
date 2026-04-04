package ziply.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseCreateResponse {
    
    /**
     * 성공적으로 생성된 집들의 ID
     */
    private List<Long> createdHouseIds;
    
    /**
     * 실패한 집들의 정보
     */
    private List<FailedHouse> failedHouses;
    
    /**
     * 전체 성공 여부
     */
    private boolean allSuccess;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedHouse {
        /**
         * 요청 배열에서의 인덱스 (0부터 시작)
         */
        private int index;
        
        /**
         * 실패한 주소
         */
        private String address;
        
        /**
         * 실패 이유
         */
        private String reason;
    }
}
