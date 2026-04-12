package ziply.review.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ziply.review.dto.response.DemoResetResponse;
import ziply.review.service.DemoService;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DemoController 테스트")
class DemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DemoService demoService;

    private Long userId;
    private DemoResetResponse successResponse;
    private DemoResetResponse emptyResponse;

    @BeforeEach
    void setUp() {
        userId = 999L;

        successResponse = DemoResetResponse.builder()
                .message("데모 데이터가 초기화되었습니다.")
                .deletedData(DemoResetResponse.DeletedData.builder()
                        .searchCards(2)
                        .houses(5)
                        .measurements(12)
                        .images(8)
                        .build())
                .build();

        emptyResponse = DemoResetResponse.builder()
                .message("삭제할 데이터가 없습니다.")
                .deletedData(DemoResetResponse.DeletedData.builder()
                        .searchCards(0)
                        .houses(0)
                        .measurements(0)
                        .images(0)
                        .build())
                .build();
    }

    @Test
    @DisplayName("데모 데이터 초기화 성공 - 데이터 있음")
    void resetDemoData_Success() throws Exception {
        // given
        when(demoService.resetDemoData(anyLong())).thenReturn(successResponse);

        // when & then
        mockMvc.perform(delete("/api/v1/demo/reset")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("데모 데이터가 초기화되었습니다."))
                .andExpect(jsonPath("$.deletedData.searchCards").value(2))
                .andExpect(jsonPath("$.deletedData.houses").value(5))
                .andExpect(jsonPath("$.deletedData.measurements").value(12))
                .andExpect(jsonPath("$.deletedData.images").value(8));
    }

    @Test
    @DisplayName("데모 데이터 초기화 성공 - 삭제할 데이터 없음")
    void resetDemoData_NoData() throws Exception {
        // given
        when(demoService.resetDemoData(anyLong())).thenReturn(emptyResponse);

        // when & then
        mockMvc.perform(delete("/api/v1/demo/reset")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("삭제할 데이터가 없습니다."))
                .andExpect(jsonPath("$.deletedData.searchCards").value(0))
                .andExpect(jsonPath("$.deletedData.houses").value(0))
                .andExpect(jsonPath("$.deletedData.measurements").value(0))
                .andExpect(jsonPath("$.deletedData.images").value(0));
    }
}
