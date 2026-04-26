package ziply.analysis.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.analysis.domain.HouseAnalysis;
import ziply.analysis.domain.SafetyNews;
import ziply.analysis.dto.response.SafetyNewsResponse;
import ziply.analysis.dto.response.SafetyNewsResponse.NewsItem;
import ziply.analysis.repository.HouseRouteAnalysisRepository;
import ziply.analysis.repository.SafetyNewsRepository;
import ziply.analysis.util.HouseLabelMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class SafetyNewsService {

    private static final Map<Integer, String> LEVEL_NAME = Map.of(
        1, "생활 불편 / 무질서",
        2, "안전 불안 / 재산 위협",
        3, "신변 위협 / 강력 범죄"
    );

    private final SafetyNewsRepository safetyNewsRepository;
    private final HouseRouteAnalysisRepository routeAnalysisRepository;
    private final CardOwnershipValidator cardOwnershipValidator;
    private final HouseLabelMapper houseLabelMapper;

    /**
     * 탐색카드 내 각 집의 동(regionName) 기반으로 치안 뉴스를 집계하여 반환.
     *
     * @param searchCardId 탐색카드 ID
     * @param userId       요청 사용자 ID (권한 확인)
     * @param period       조회 기간 (개월): 3, 6, 12
     * @param level        뉴스 레벨 (1: 생활 불편, 2: 안전 불안, 3: 신변 위협)
     * @param page         페이지 번호 (0-based)
     * @param size         페이지당 항목 수
     */
    @Transactional(readOnly = true)
    public List<SafetyNewsResponse> getNewsAnalysis(UUID searchCardId, Long userId,
                                                     int period, int level, int page, int size) {
        cardOwnershipValidator.validate(searchCardId, userId);

        List<HouseAnalysis> allData = routeAnalysisRepository.findBySearchCardId(searchCardId);
        if (allData.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> labelMap = houseLabelMapper.build(allData);

        Map<Long, String> regionMap = allData.stream()
                .filter(ha -> ha.getRegionName() != null)
                .collect(Collectors.toMap(
                        HouseAnalysis::getHouseId,
                        HouseAnalysis::getRegionName,
                        (a, b) -> a
                ));

        LocalDateTime since = LocalDateTime.now().minusMonths(period);

        return labelMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(entry -> {
                    Long houseId = entry.getKey();
                    String label = entry.getValue();
                    String regionName = regionMap.get(houseId);

                    if (regionName == null || regionName.isBlank()) {
                        log.warn("[SafetyNews] houseId={} regionName 없음, 집계 불가", houseId);
                        return buildEmptyResponse(label);
                    }

                    return buildNewsResponse(label, regionName, period, since, level, page, size);
                })
                .collect(Collectors.toList());
    }

    private SafetyNewsResponse buildNewsResponse(String label, String regionName,
                                                  int period, LocalDateTime since,
                                                  int level, int page, int size) {
        List<SafetyNews> allNews = safetyNewsRepository.findByRegionAndPeriod(regionName, since);
        int level1 = 0, level2 = 0, level3 = 0;
        for (SafetyNews n : allNews) {
            switch (n.getCategoryLevel()) {
                case 1 -> level1++;
                case 2 -> level2++;
                case 3 -> level3++;
            }
        }
        int total = level1 + level2 + level3;

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<SafetyNews> newsPage = safetyNewsRepository.findByRegionAndPeriodAndLevel(regionName, since, level, pageable);

        String message = buildMessage(regionName, period, level1, level2, level3, total);

        return SafetyNewsResponse.builder()
                .label(label)
                .regionName(regionName)
                .level1Count(level1)
                .level2Count(level2)
                .level3Count(level3)
                .message(message)
                .news(toNewsItems(newsPage))
                .page(newsPage.getNumber())
                .size(newsPage.getSize())
                .totalCount(newsPage.getTotalElements())
                .totalPages(newsPage.getTotalPages())
                .build();
    }

    private List<NewsItem> toNewsItems(Page<SafetyNews> newsPage) {
        return newsPage.getContent().stream()
                .map(n -> NewsItem.builder()
                        .title(n.getTitle())
                        .categoryLevel(LEVEL_NAME.getOrDefault(n.getCategoryLevel(), "알 수 없음"))
                        .categoryTag(n.getCategoryTag())
                        .publishedAt(n.getPublishedAt().toLocalDate())
                        .contentUrl(n.getContentUrl())
                        .summary(n.getSummary())
                        .build())
                .collect(Collectors.toList());
    }

    private SafetyNewsResponse buildEmptyResponse(String label) {
        return SafetyNewsResponse.builder()
                .label(label)
                .regionName(null)
                .level1Count(0).level2Count(0).level3Count(0)
                .message("이 집의 지역 정보를 확인할 수 없어 치안 뉴스를 불러오지 못했어요.")
                .news(Collections.emptyList())
                .page(0).size(0).totalCount(0).totalPages(0)
                .build();
    }

    private String buildMessage(String regionName, int period,
                                int level1, int level2, int level3, int total) {
        if (total == 0) {
            return String.format("%s 인근에서 최근 %d개월간 수집된 치안 관련 뉴스가 없어요.", regionName, period);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s 인근에서 최근 %d개월간 치안 뉴스 %d건이 수집됐어요. ", regionName, period, total));

        if (level3 > 0) {
            sb.append(String.format("신변 위협 %d건이 포함되어 있으니 주의하세요.", level3));
        } else if (level2 > 0) {
            sb.append(String.format("안전 불안 %d건, 생활 불편 %d건이 보고됐어요.", level2, level1));
        } else {
            sb.append(String.format("생활 불편 %d건으로 비교적 안전한 편이에요.", level1));
        }

        return sb.toString();
    }
}
