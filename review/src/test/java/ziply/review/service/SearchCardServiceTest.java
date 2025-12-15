package ziply.review.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ziply.review.domain.BasePoint;
import ziply.review.domain.SearchCard;
import ziply.review.domain.SearchCardStatus;
import ziply.review.dto.request.SearchCardCreateRequest;
import ziply.review.repository.SearchCardRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchCardServiceTest {

    @Mock
    private SearchCardRepository searchCardRepository;

    @InjectMocks
    private SearchCardService searchCardService;

    @Test
    @DisplayName("기점 없이 탐색 카드를 생성하면 카드만 저장된다")
    void createSearchCardWithoutBasePoints() {
        // given
        Long userId = 1L;
        SearchCardCreateRequest request = new SearchCardCreateRequest();
        request.setTitle("서울 탐색");
        request.setStartDate(LocalDate.of(2024, 1, 1));
        request.setEndDate(LocalDate.of(2024, 1, 31));
        request.setBasePoints(null);

        SearchCard savedCard = new SearchCard(
                userId,
                request.getTitle(),
                request.getStartDate(),
                request.getEndDate()
        );

        when(searchCardRepository.save(any(SearchCard.class))).thenReturn(savedCard);

        // when
        Long cardId = searchCardService.createSearchCard(userId, request);

        // then
        assertThat(cardId).isNotNull();
        verify(searchCardRepository, times(1)).save(any(SearchCard.class));

        ArgumentCaptor<SearchCard> captor = ArgumentCaptor.forClass(SearchCard.class);
        verify(searchCardRepository).save(captor.capture());
        SearchCard capturedCard = captor.getValue();
        assertThat(capturedCard.getUserId()).isEqualTo(userId);
        assertThat(capturedCard.getTitle()).isEqualTo("서울 탐색");
        assertThat(capturedCard.getStatus()).isEqualTo(SearchCardStatus.PLANNED);
    }

    @Test
    @DisplayName("기점과 함께 탐색 카드를 생성하면 카드와 기점이 모두 저장된다")
    void createSearchCardWithBasePoints() {
        // given
        Long userId = 1L;
        SearchCardCreateRequest request = new SearchCardCreateRequest();
        request.setTitle("서울 탐색");
        request.setStartDate(LocalDate.of(2024, 1, 1));
        request.setEndDate(LocalDate.of(2024, 1, 31));

        List<SearchCardCreateRequest.BasePointRequest> basePointRequests = new ArrayList<>();
        SearchCardCreateRequest.BasePointRequest basePointRequest1 = new SearchCardCreateRequest.BasePointRequest();
        basePointRequest1.setAlias("회사");
        basePointRequest1.setAddress("서울시 강남구");
        basePointRequest1.setLatitude(37.5665);
        basePointRequest1.setLongitude(126.9780);
        basePointRequests.add(basePointRequest1);

        SearchCardCreateRequest.BasePointRequest basePointRequest2 = new SearchCardCreateRequest.BasePointRequest();
        basePointRequest2.setAlias("학교");
        basePointRequest2.setAddress("서울시 서초구");
        basePointRequest2.setLatitude(37.4837);
        basePointRequest2.setLongitude(127.0324);
        basePointRequests.add(basePointRequest2);

        request.setBasePoints(basePointRequests);

        SearchCard savedCard = new SearchCard(
                userId,
                request.getTitle(),
                request.getStartDate(),
                request.getEndDate()
        );
        savedCard.addBasePoint(new BasePoint(
                basePointRequest1.getAlias(),
                basePointRequest1.getAddress(),
                basePointRequest1.getLatitude(),
                basePointRequest1.getLongitude()
        ));
        savedCard.addBasePoint(new BasePoint(
                basePointRequest2.getAlias(),
                basePointRequest2.getAddress(),
                basePointRequest2.getLatitude(),
                basePointRequest2.getLongitude()
        ));

        when(searchCardRepository.save(any(SearchCard.class))).thenReturn(savedCard);

        // when
        Long cardId = searchCardService.createSearchCard(userId, request);

        // then
        assertThat(cardId).isNotNull();
        verify(searchCardRepository, times(1)).save(any(SearchCard.class));

        ArgumentCaptor<SearchCard> captor = ArgumentCaptor.forClass(SearchCard.class);
        verify(searchCardRepository).save(captor.capture());
        SearchCard capturedCard = captor.getValue();
        assertThat(capturedCard.getUserId()).isEqualTo(userId);
        assertThat(capturedCard.getTitle()).isEqualTo("서울 탐색");
        assertThat(capturedCard.getBasePoints()).hasSize(2);
        assertThat(capturedCard.getBasePoints().get(0).getAlias()).isEqualTo("회사");
        assertThat(capturedCard.getBasePoints().get(1).getAlias()).isEqualTo("학교");
    }

    @Test
    @DisplayName("빈 기점 리스트와 함께 탐색 카드를 생성하면 카드만 저장된다")
    void createSearchCardWithEmptyBasePoints() {
        // given
        Long userId = 1L;
        SearchCardCreateRequest request = new SearchCardCreateRequest();
        request.setTitle("서울 탐색");
        request.setStartDate(LocalDate.of(2024, 1, 1));
        request.setEndDate(LocalDate.of(2024, 1, 31));
        request.setBasePoints(new ArrayList<>());

        SearchCard savedCard = new SearchCard(
                userId,
                request.getTitle(),
                request.getStartDate(),
                request.getEndDate()
        );

        when(searchCardRepository.save(any(SearchCard.class))).thenReturn(savedCard);

        // when
        Long cardId = searchCardService.createSearchCard(userId, request);

        // then
        assertThat(cardId).isNotNull();
        verify(searchCardRepository, times(1)).save(any(SearchCard.class));

        ArgumentCaptor<SearchCard> captor = ArgumentCaptor.forClass(SearchCard.class);
        verify(searchCardRepository).save(captor.capture());
        SearchCard capturedCard = captor.getValue();
        assertThat(capturedCard.getBasePoints()).isEmpty();
    }
}


