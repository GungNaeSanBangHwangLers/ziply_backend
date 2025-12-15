package ziply.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ziply.review.dto.request.SearchCardCreateRequest;
import ziply.review.service.SearchCardService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchCardController.class)
@AutoConfigureMockMvc(addFilters = false)
class SearchCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchCardService searchCardService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/review/card : 탐색 카드 생성 시 200과 카드 ID 응답")
    void createSearchCardSuccess() throws Exception {
        // given
        Long userId = 1L;
        Long expectedCardId = 100L;

        SearchCardCreateRequest request = new SearchCardCreateRequest();
        request.setTitle("서울 탐색");
        request.setStartDate(LocalDate.of(2024, 1, 1));
        request.setEndDate(LocalDate.of(2024, 1, 31));

        List<SearchCardCreateRequest.BasePointRequest> basePoints = new ArrayList<>();
        SearchCardCreateRequest.BasePointRequest basePoint = new SearchCardCreateRequest.BasePointRequest();
        basePoint.setAlias("회사");
        basePoint.setAddress("서울시 강남구");
        basePoint.setLatitude(37.5665);
        basePoint.setLongitude(126.9780);
        basePoints.add(basePoint);
        request.setBasePoints(basePoints);

        when(searchCardService.createSearchCard(eq(userId), any(SearchCardCreateRequest.class)))
                .thenReturn(expectedCardId);

        // when & then
        mockMvc.perform(
                        post("/api/v1/review/card")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .requestAttr("userId", userId)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(expectedCardId));
    }

    @Test
    @DisplayName("POST /api/v1/review/card : 기점 없이 탐색 카드 생성 시 200 응답")
    void createSearchCardWithoutBasePoints() throws Exception {
        // given
        Long userId = 1L;
        Long expectedCardId = 200L;

        SearchCardCreateRequest request = new SearchCardCreateRequest();
        request.setTitle("부산 탐색");
        request.setStartDate(LocalDate.of(2024, 2, 1));
        request.setEndDate(LocalDate.of(2024, 2, 28));
        request.setBasePoints(null);

        when(searchCardService.createSearchCard(eq(userId), any(SearchCardCreateRequest.class)))
                .thenReturn(expectedCardId);

        // when & then
        mockMvc.perform(
                        post("/api/v1/review/card")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .requestAttr("userId", userId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(expectedCardId));
    }
}

