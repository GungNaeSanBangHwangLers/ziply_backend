package ziply.analysis.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ziply.analysis.domain.HouseAnalysis;
import ziply.analysis.domain.SafetyNews;
import ziply.analysis.dto.response.SafetyNewsResponse;
import ziply.analysis.dto.response.SafetyNewsResponse.NewsItem;
import ziply.analysis.repository.HouseRouteAnalysisRepository;
import ziply.analysis.repository.SafetyNewsRepository;

import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
@RequiredArgsConstructor
public class SafetyNewsService {

    private static final int MAX_RECENT_NEWS = 5;

    private final SafetyNewsRepository safetyNewsRepository;
    private final HouseRouteAnalysisRepository routeAnalysisRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${services.review.url:http://review-service:8080}")
    private String reviewServiceUrl;

    /**
     * 탐색카드 내 각 집의 동(regionName) 기반으로 치안 뉴스를 집계하여 반환.
     *
     * @param searchCardId 탐색카드 ID
     * @param userId       요청 사용자 ID (권한 확인)
     * @param period       조회 기간 (개월): 3, 6, 12
     */
    @Transactional(readOnly = true)
    public List<SafetyNewsResponse> getNewsAnalysis(UUID searchCardId, Long userId, int period) {
        checkCardOwner(searchCardId, userId);

        // HouseAnalysis에서 searchCard 소속 집 목록 및 regionName 조회
        List<HouseAnalysis> allData = routeAnalysisRepository.findBySearchCardId(searchCardId);
        if (allData.isEmpty()) {
            return Collections.emptyList();
        }

        // houseId → label (A, B, C...)
        Map<Long, String> labelMap = buildLabelMap(allData);

        // houseId → regionName (중복 제거, 첫 번째 값 사용)
        Map<Long, String> regionMap = allData.stream()
                .filter(ha -> ha.getRegionName() != null)
                .collect(Collectors.toMap(
                        HouseAnalysis::getHouseId,
                        HouseAnalysis::getRegionName,
                        (a, b) -> a   // 중복 시 첫 번째 유지
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
                        return buildEmptyResponse(houseId, label, period);
                    }

                    return buildNewsResponse(houseId, label, regionName, period, since);
                })
                .collect(Collectors.toList());
    }

    private SafetyNewsResponse buildNewsResponse(Long houseId, String label, String regionName,
                                                  int period, LocalDateTime since) {
        List<SafetyNews> newsList = safetyNewsRepository.findByRegionAndPeriod(regionName, since);

        int level1 = 0, level2 = 0, level3 = 0;
        for (SafetyNews n : newsList) {
            switch (n.getCategoryLevel()) {
                case 1 -> level1++;
                case 2 -> level2++;
                case 3 -> level3++;
            }
        }
        int total = level1 + level2 + level3;

        List<NewsItem> recent = newsList.stream()
                .limit(MAX_RECENT_NEWS)
                .map(n -> NewsItem.builder()
                        .id(n.getId())
                        .title(n.getTitle())
                        .categoryLevel(n.getCategoryLevel())
                        .publishedAt(n.getPublishedAt())
                        .contentUrl(n.getContentUrl())
                        .summary(n.getSummary())
                        .build())
                .collect(Collectors.toList());

        String message = buildMessage(regionName, period, level1, level2, level3, total);

        return SafetyNewsResponse.builder()
                .houseId(houseId)
                .label(label)
                .regionName(regionName)
                .period(period)
                .level1Count(level1)
                .level2Count(level2)
                .level3Count(level3)
                .totalCount(total)
                .message(message)
                .recentNews(recent)
                .build();
    }

    private SafetyNewsResponse buildEmptyResponse(Long houseId, String label, int period) {
        return SafetyNewsResponse.builder()
                .houseId(houseId)
                .label(label)
                .regionName(null)
                .period(period)
                .level1Count(0).level2Count(0).level3Count(0).totalCount(0)
                .message("이 집의 지역 정보를 확인할 수 없어 치안 뉴스를 불러오지 못했어요.")
                .recentNews(Collections.emptyList())
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

    private Map<Long, String> buildLabelMap(List<HouseAnalysis> allData) {
        List<Long> sortedIds = allData.stream()
                .map(HouseAnalysis::getHouseId)
                .distinct().sorted().toList();
        Map<Long, String> map = new HashMap<>();
        for (int i = 0; i < sortedIds.size(); i++) {
            map.put(sortedIds.get(i), String.valueOf((char) ('A' + i)));
        }
        return map;
    }

    private void checkCardOwner(UUID searchCardId, Long userId) {
        webClientBuilder.build().get()
                .uri(reviewServiceUrl + "/api/v1/review/card/{searchCardId}/owner-check?userId={userId}",
                        searchCardId, userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        cr -> Mono.error(new AccessDeniedException("권한 확인 실패")))
                .bodyToMono(Boolean.class)
                .map(isOwner -> {
                    if (!isOwner) throw new AccessDeniedException("접근 권한이 없습니다.");
                    return isOwner;
                }).block();
    }
}
