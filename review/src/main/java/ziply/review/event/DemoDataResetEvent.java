package ziply.review.event;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 데모 데이터 초기화 이벤트
 * Review → Analysis로 전달
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DemoDataResetEvent {
    private Long userId;
    private List<Long> houseIds;
    private Long timestamp;
}
