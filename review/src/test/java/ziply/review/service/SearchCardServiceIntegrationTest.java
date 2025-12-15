package ziply.review.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ziply.review.domain.BasePoint;
import ziply.review.domain.SearchCard;
import ziply.review.domain.SearchCardStatus;
import ziply.review.dto.request.SearchCardCreateRequest;
import ziply.review.repository.SearchCardRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SearchCardServiceIntegrationTest {

    @Autowired
    private SearchCardService searchCardService;

    @Autowired
    private SearchCardRepository searchCardRepository;

    @Test
    @DisplayName("기점 없이 탐색 카드를 생성하면 DB에 저장된다")
    void createSearchCardWithoutBasePointsSavesToDatabase() {
        // given
        Long userId = 1L;
        SearchCardCreateRequest request = new SearchCardCreateRequest();
        request.setTitle("서울 탐색");
        request.setStartDate(LocalDate.of(2024, 1, 1));
        request.setEndDate(LocalDate.of(2024, 1, 31));
        request.setBasePoints(null);

        // when
        Long cardId = searchCardService.createSearchCard(userId, request);

        // then
        assertThat(cardId).isNotNull();

        SearchCard savedCard = searchCardRepository.findById(cardId)
                .orElseThrow(() -> new AssertionError("탐색 카드가 DB에 저장되지 않았습니다"));
        assertThat(savedCard.getUserId()).isEqualTo(userId);
        assertThat(savedCard.getTitle()).isEqualTo("서울 탐색");
        assertThat(savedCard.getStartDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(savedCard.getEndDate()).isEqualTo(LocalDate.of(2024, 1, 31));
        assertThat(savedCard.getStatus()).isEqualTo(SearchCardStatus.PLANNED);
        assertThat(savedCard.getBasePoints()).isEmpty();
    }

    @Test
    @DisplayName("기점과 함께 탐색 카드를 생성하면 카드와 기점이 모두 DB에 저장된다")
    void createSearchCardWithBasePointsSavesToDatabase() {
        // given
        Long userId = 1L;
        SearchCardCreateRequest request = new SearchCardCreateRequest();
        request.setTitle("부산 탐색");
        request.setStartDate(LocalDate.of(2024, 2, 1));
        request.setEndDate(LocalDate.of(2024, 2, 28));

        List<SearchCardCreateRequest.BasePointRequest> basePoints = new ArrayList<>();
        SearchCardCreateRequest.BasePointRequest basePoint1 = new SearchCardCreateRequest.BasePointRequest();
        basePoint1.setAlias("회사");
        basePoint1.setAddress("서울시 강남구");
        basePoint1.setLatitude(37.5665);
        basePoint1.setLongitude(126.9780);
        basePoints.add(basePoint1);

        SearchCardCreateRequest.BasePointRequest basePoint2 = new SearchCardCreateRequest.BasePointRequest();
        basePoint2.setAlias("학교");
        basePoint2.setAddress("서울시 서초구");
        basePoint2.setLatitude(37.4837);
        basePoint2.setLongitude(127.0324);
        basePoints.add(basePoint2);

        request.setBasePoints(basePoints);

        // when
        Long cardId = searchCardService.createSearchCard(userId, request);

        // then
        assertThat(cardId).isNotNull();

        SearchCard savedCard = searchCardRepository.findById(cardId)
                .orElseThrow(() -> new AssertionError("탐색 카드가 DB에 저장되지 않았습니다"));
        assertThat(savedCard.getTitle()).isEqualTo("부산 탐색");
        assertThat(savedCard.getBasePoints()).hasSize(2);

        BasePoint savedBasePoint1 = savedCard.getBasePoints().get(0);
        assertThat(savedBasePoint1.getAlias()).isEqualTo("회사");
        assertThat(savedBasePoint1.getAddress()).isEqualTo("서울시 강남구");
        assertThat(savedBasePoint1.getLatitude()).isEqualTo(37.5665);
        assertThat(savedBasePoint1.getLongitude()).isEqualTo(126.9780);
        assertThat(savedBasePoint1.getSearchCard()).isEqualTo(savedCard);

        BasePoint savedBasePoint2 = savedCard.getBasePoints().get(1);
        assertThat(savedBasePoint2.getAlias()).isEqualTo("학교");
        assertThat(savedBasePoint2.getAddress()).isEqualTo("서울시 서초구");
        assertThat(savedBasePoint2.getLatitude()).isEqualTo(37.4837);
        assertThat(savedBasePoint2.getLongitude()).isEqualTo(127.0324);
        assertThat(savedBasePoint2.getSearchCard()).isEqualTo(savedCard);
    }

    @Test
    @DisplayName("여러 개의 탐색 카드를 생성하면 모두 독립적으로 저장된다")
    void createMultipleSearchCardsSavesIndependently() {
        // given
        Long userId = 1L;

        SearchCardCreateRequest request1 = new SearchCardCreateRequest();
        request1.setTitle("첫 번째 카드");
        request1.setStartDate(LocalDate.of(2024, 1, 1));
        request1.setEndDate(LocalDate.of(2024, 1, 31));

        SearchCardCreateRequest request2 = new SearchCardCreateRequest();
        request2.setTitle("두 번째 카드");
        request2.setStartDate(LocalDate.of(2024, 2, 1));
        request2.setEndDate(LocalDate.of(2024, 2, 28));

        // when
        Long cardId1 = searchCardService.createSearchCard(userId, request1);
        Long cardId2 = searchCardService.createSearchCard(userId, request2);

        // then
        assertThat(cardId1).isNotEqualTo(cardId2);

        SearchCard card1 = searchCardRepository.findById(cardId1)
                .orElseThrow(() -> new AssertionError("첫 번째 카드를 찾을 수 없습니다"));
        SearchCard card2 = searchCardRepository.findById(cardId2)
                .orElseThrow(() -> new AssertionError("두 번째 카드를 찾을 수 없습니다"));

        assertThat(card1.getTitle()).isEqualTo("첫 번째 카드");
        assertThat(card2.getTitle()).isEqualTo("두 번째 카드");
    }
}


