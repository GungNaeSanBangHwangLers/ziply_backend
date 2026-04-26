package ziply.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import ziply.analysis.domain.HouseAnalysis;
import ziply.analysis.domain.SafetyNews;
import ziply.analysis.dto.response.SafetyNewsResponse;
import ziply.analysis.repository.HouseRouteAnalysisRepository;
import ziply.analysis.repository.SafetyNewsRepository;
import ziply.analysis.util.HouseLabelMapper;

@ExtendWith(MockitoExtension.class)
class SafetyNewsServiceTest {

    @Mock
    private SafetyNewsRepository safetyNewsRepository;
    @Mock
    private HouseRouteAnalysisRepository routeAnalysisRepository;
    @Mock
    private CardOwnershipValidator cardOwnershipValidator;

    private SafetyNewsService safetyNewsService;

    @BeforeEach
    void setUp() {
        safetyNewsService = new SafetyNewsService(
                safetyNewsRepository,
                routeAnalysisRepository,
                cardOwnershipValidator,
                new HouseLabelMapper()
        );
    }

    // ───────────── 권한 확인 ─────────────

    @Test
    void getNewsAnalysis_ThrowsWhenUserIsNotOwner() {
        UUID searchCardId = UUID.randomUUID();
        doThrow(new AccessDeniedException("접근 권한이 없습니다."))
                .when(cardOwnershipValidator).validate(searchCardId, 1L);

        assertThatThrownBy(() -> safetyNewsService.getNewsAnalysis(searchCardId, 1L, 3, 0, 10))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ───────────── 빈 데이터 ─────────────

    @Test
    void getNewsAnalysis_ReturnsEmptyWhenNoHouseData() {
        UUID searchCardId = UUID.randomUUID();
        when(routeAnalysisRepository.findBySearchCardId(searchCardId)).thenReturn(List.of());

        List<SafetyNewsResponse> result = safetyNewsService.getNewsAnalysis(searchCardId, 1L, 3, 0, 10);

        assertThat(result).isEmpty();
    }

    @Test
    void getNewsAnalysis_ReturnsEmptyResponseWhenRegionNameIsNull() {
        UUID searchCardId = UUID.randomUUID();

        HouseAnalysis house = HouseAnalysis.builder()
                .searchCardId(searchCardId)
                .houseId(10L)
                .basePointId(1L)
                .regionName(null)
                .build();
        when(routeAnalysisRepository.findBySearchCardId(searchCardId)).thenReturn(List.of(house));

        List<SafetyNewsResponse> result = safetyNewsService.getNewsAnalysis(searchCardId, 1L, 3, 0, 10);

        assertThat(result).hasSize(1);
        SafetyNewsResponse resp = result.get(0);
        assertThat(resp.getLabel()).isEqualTo("A");
        assertThat(resp.getRegionName()).isNull();
        assertThat(resp.getLevel1Count()).isZero();
        assertThat(resp.getLevel2Count()).isZero();
        assertThat(resp.getLevel3Count()).isZero();
        assertThat(resp.getRecentNews()).isEmpty();
        assertThat(resp.getMessage()).contains("지역 정보를 확인할 수 없어");
    }

    // ───────────── 레벨 카운트 ─────────────

    @Test
    void getNewsAnalysis_CountsNewsByLevel() {
        UUID searchCardId = UUID.randomUUID();

        HouseAnalysis house = makeHouse(searchCardId, 10L, "상도동");
        when(routeAnalysisRepository.findBySearchCardId(searchCardId)).thenReturn(List.of(house));

        SafetyNews n1 = makeNews(1, "생활 소음", LocalDateTime.now().minusDays(1));
        SafetyNews n2 = makeNews(1, "쓰레기 무단투기", LocalDateTime.now().minusDays(2));
        SafetyNews n3 = makeNews(2, "절도", LocalDateTime.now().minusDays(3));
        SafetyNews n4 = makeNews(3, "폭행", LocalDateTime.now().minusDays(4));
        List<SafetyNews> allNews = List.of(n1, n2, n3, n4);

        when(safetyNewsRepository.findByRegionAndPeriod(eq("상도동"), any(LocalDateTime.class)))
                .thenReturn(allNews);
        when(safetyNewsRepository.findByRegionAndPeriodPageable(eq("상도동"), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(allNews));

        List<SafetyNewsResponse> result = safetyNewsService.getNewsAnalysis(searchCardId, 1L, 3, 0, 10);

        SafetyNewsResponse resp = result.get(0);
        assertThat(resp.getLevel1Count()).isEqualTo(2);
        assertThat(resp.getLevel2Count()).isEqualTo(1);
        assertThat(resp.getLevel3Count()).isEqualTo(1);
    }

    // ───────────── NewsItem 변환 ─────────────

    @Test
    void getNewsAnalysis_MapsNewsItemFieldsCorrectly() {
        UUID searchCardId = UUID.randomUUID();

        HouseAnalysis house = makeHouse(searchCardId, 10L, "상도동");
        when(routeAnalysisRepository.findBySearchCardId(searchCardId)).thenReturn(List.of(house));

        LocalDateTime published = LocalDateTime.of(2026, 1, 15, 9, 0);
        SafetyNews news = makeNews(2, "재산 범죄", published);
        ReflectionTestUtils.setField(news, "title", "상도동 절도 사건 발생");
        ReflectionTestUtils.setField(news, "contentUrl", "http://news.example.com/123");
        ReflectionTestUtils.setField(news, "summary", "상도동에서 절도 사건이 발생했습니다.");

        when(safetyNewsRepository.findByRegionAndPeriod(anyString(), any())).thenReturn(List.of(news));
        when(safetyNewsRepository.findByRegionAndPeriodPageable(anyString(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(news)));

        List<SafetyNewsResponse> result = safetyNewsService.getNewsAnalysis(searchCardId, 1L, 3, 0, 10);

        SafetyNewsResponse.NewsItem item = result.get(0).getRecentNews().get(0);
        assertThat(item.getTitle()).isEqualTo("상도동 절도 사건 발생");
        assertThat(item.getCategoryLevel()).isEqualTo("안전 불안 / 재산 위협");
        assertThat(item.getCategoryTag()).isEqualTo("재산 범죄");
        assertThat(item.getPublishedAt()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(item.getContentUrl()).isEqualTo("http://news.example.com/123");
        assertThat(item.getSummary()).isEqualTo("상도동에서 절도 사건이 발생했습니다.");
    }

    // ───────────── 페이지네이션 메타 ─────────────

    @Test
    void getNewsAnalysis_ReflectsPageMetaInResponse() {
        UUID searchCardId = UUID.randomUUID();

        HouseAnalysis house = makeHouse(searchCardId, 10L, "상도동");
        when(routeAnalysisRepository.findBySearchCardId(searchCardId)).thenReturn(List.of(house));

        SafetyNews n = makeNews(1, "소음", LocalDateTime.now().minusDays(1));
        List<SafetyNews> pageContent = List.of(n);

        when(safetyNewsRepository.findByRegionAndPeriod(anyString(), any())).thenReturn(pageContent);
        Page<SafetyNews> page = new PageImpl<>(pageContent,
                org.springframework.data.domain.PageRequest.of(1, 5), 15);
        when(safetyNewsRepository.findByRegionAndPeriodPageable(anyString(), any(), any()))
                .thenReturn(page);

        List<SafetyNewsResponse> result = safetyNewsService.getNewsAnalysis(searchCardId, 1L, 3, 1, 5);

        SafetyNewsResponse resp = result.get(0);
        assertThat(resp.getPage()).isEqualTo(1);
        assertThat(resp.getSize()).isEqualTo(5);
        assertThat(resp.getTotalCount()).isEqualTo(15);
        assertThat(resp.getTotalPages()).isEqualTo(3);
    }

    // ───────────── 메시지 ─────────────

    @Test
    void getNewsAnalysis_MessageContainsLevel3Warning() {
        UUID searchCardId = UUID.randomUUID();

        HouseAnalysis house = makeHouse(searchCardId, 10L, "상도동");
        when(routeAnalysisRepository.findBySearchCardId(searchCardId)).thenReturn(List.of(house));

        SafetyNews n = makeNews(3, "강력 범죄", LocalDateTime.now().minusDays(1));
        when(safetyNewsRepository.findByRegionAndPeriod(anyString(), any())).thenReturn(List.of(n));
        when(safetyNewsRepository.findByRegionAndPeriodPageable(anyString(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(n)));

        List<SafetyNewsResponse> result = safetyNewsService.getNewsAnalysis(searchCardId, 1L, 3, 0, 10);

        assertThat(result.get(0).getMessage()).contains("신변 위협").contains("주의하세요");
    }

    @Test
    void getNewsAnalysis_MessageContainsLevel2Summary() {
        UUID searchCardId = UUID.randomUUID();

        HouseAnalysis house = makeHouse(searchCardId, 10L, "상도동");
        when(routeAnalysisRepository.findBySearchCardId(searchCardId)).thenReturn(List.of(house));

        SafetyNews n1 = makeNews(2, "절도", LocalDateTime.now().minusDays(1));
        SafetyNews n2 = makeNews(1, "소음", LocalDateTime.now().minusDays(2));
        List<SafetyNews> news = List.of(n1, n2);

        when(safetyNewsRepository.findByRegionAndPeriod(anyString(), any())).thenReturn(news);
        when(safetyNewsRepository.findByRegionAndPeriodPageable(anyString(), any(), any()))
                .thenReturn(new PageImpl<>(news));

        List<SafetyNewsResponse> result = safetyNewsService.getNewsAnalysis(searchCardId, 1L, 3, 0, 10);

        assertThat(result.get(0).getMessage()).contains("안전 불안").contains("생활 불편");
    }

    @Test
    void getNewsAnalysis_MessageIndicatesNoNewsWhenEmpty() {
        UUID searchCardId = UUID.randomUUID();

        HouseAnalysis house = makeHouse(searchCardId, 10L, "상도동");
        when(routeAnalysisRepository.findBySearchCardId(searchCardId)).thenReturn(List.of(house));

        when(safetyNewsRepository.findByRegionAndPeriod(anyString(), any())).thenReturn(List.of());
        when(safetyNewsRepository.findByRegionAndPeriodPageable(anyString(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        List<SafetyNewsResponse> result = safetyNewsService.getNewsAnalysis(searchCardId, 1L, 3, 0, 10);

        assertThat(result.get(0).getMessage()).contains("수집된 치안 관련 뉴스가 없어요");
    }

    // ───────────── 레이블 순서 ─────────────

    @Test
    void getNewsAnalysis_AssignsLabelsInHouseIdOrder() {
        UUID searchCardId = UUID.randomUUID();

        HouseAnalysis h1 = makeHouse(searchCardId, 30L, "상도동");
        HouseAnalysis h2 = makeHouse(searchCardId, 10L, "동작동");
        HouseAnalysis h3 = makeHouse(searchCardId, 20L, "노량진동");
        when(routeAnalysisRepository.findBySearchCardId(searchCardId)).thenReturn(List.of(h1, h2, h3));

        when(safetyNewsRepository.findByRegionAndPeriod(anyString(), any())).thenReturn(List.of());
        when(safetyNewsRepository.findByRegionAndPeriodPageable(anyString(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        List<SafetyNewsResponse> result = safetyNewsService.getNewsAnalysis(searchCardId, 1L, 3, 0, 10);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getLabel()).isEqualTo("A");
        assertThat(result.get(1).getLabel()).isEqualTo("B");
        assertThat(result.get(2).getLabel()).isEqualTo("C");
    }

    // ───────────── 헬퍼 ─────────────

    private HouseAnalysis makeHouse(UUID searchCardId, Long houseId, String regionName) {
        return HouseAnalysis.builder()
                .searchCardId(searchCardId)
                .houseId(houseId)
                .basePointId(1L)
                .regionName(regionName)
                .build();
    }

    private SafetyNews makeNews(int level, String tag, LocalDateTime publishedAt) {
        SafetyNews news = new SafetyNews();
        ReflectionTestUtils.setField(news, "id", 1L);
        ReflectionTestUtils.setField(news, "title", "테스트 뉴스 제목");
        ReflectionTestUtils.setField(news, "contentUrl", "http://news.example.com/1");
        ReflectionTestUtils.setField(news, "categoryLevel", level);
        ReflectionTestUtils.setField(news, "categoryTag", tag);
        ReflectionTestUtils.setField(news, "regionName", "상도동");
        ReflectionTestUtils.setField(news, "publishedAt", publishedAt);
        ReflectionTestUtils.setField(news, "summary", "요약 내용");
        return news;
    }
}
