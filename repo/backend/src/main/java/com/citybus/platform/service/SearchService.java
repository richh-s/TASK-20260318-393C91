package com.citybus.platform.service;

import com.citybus.platform.dto.response.RouteSearchResult;
import com.citybus.platform.dto.response.StopSearchResult;
import com.citybus.platform.entity.BusRoute;
import com.citybus.platform.entity.BusStop;
import com.citybus.platform.entity.SortingWeight;
import com.citybus.platform.repository.BusRouteRepository;
import com.citybus.platform.repository.BusStopRepository;
import com.citybus.platform.repository.SortingWeightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final BusRouteRepository routeRepository;
    private final BusStopRepository stopRepository;
    private final SortingWeightRepository weightRepository;

    @Value("${app.search.max-results:50}")
    private int maxResults;

    @Value("${app.search.autocomplete-max:10}")
    private int autocompleteMax;

    public List<RouteSearchResult> searchRoutes(String query) {
        if (query == null || query.isBlank()) return Collections.emptyList();
        log.info("Route search: query={}", query);
        return routeRepository.searchRoutes(query.trim()).stream()
                .limit(maxResults)
                .map(this::toRouteResult)
                .collect(Collectors.toList());
    }

    public List<StopSearchResult> searchStops(String query, Long routeId) {
        if (query == null || query.isBlank()) return Collections.emptyList();
        log.info("Stop search: query={}, routeId={}", query, routeId);

        List<BusStop> stops;
        if (routeId != null) {
            stops = stopRepository.searchStopsInRoute(query.trim(), routeId);
        } else {
            stops = stopRepository.searchStops(query.trim());
        }

        BigDecimal freqWeight = getWeight("frequency_score");
        BigDecimal popWeight = getWeight("popularity_score");

        return stops.stream()
                .map(s -> toStopResult(s, freqWeight, popWeight))
                .sorted(Comparator.comparingDouble(StopSearchResult::getSortScore).reversed())
                .distinct()
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    public List<String> autocomplete(String query) {
        if (query == null || query.isBlank()) return Collections.emptyList();
        List<String> suggestions = new ArrayList<>();

        stopRepository.autocompleteNames(query.trim())
                .stream().limit(autocompleteMax).forEach(suggestions::add);

        routeRepository.searchRoutes(query.trim()).stream()
                .limit(3)
                .map(r -> r.getRouteNumber() + " - " + r.getName())
                .forEach(suggestions::add);

        return suggestions.stream().distinct().limit(autocompleteMax)
                .collect(Collectors.toList());
    }

    private BigDecimal getWeight(String factorName) {
        return weightRepository.findByFactorName(factorName)
                .map(SortingWeight::getWeight)
                .orElse(BigDecimal.ONE);
    }

    private RouteSearchResult toRouteResult(BusRoute r) {
        return new RouteSearchResult(r.getId(), r.getRouteNumber(), r.getName(),
                r.getDescription(), r.getStatus(),
                r.getStops() != null ? r.getStops().size() : 0);
    }

    private StopSearchResult toStopResult(BusStop s, BigDecimal freqWeight, BigDecimal popWeight) {
        double sortScore = s.getPopularityScore() * popWeight.doubleValue();
        return new StopSearchResult(s.getId(), s.getNameEn(), s.getNameCn(),
                s.getAddress(), s.getSequenceNumber(),
                s.getRoute() != null ? s.getRoute().getId() : null,
                s.getRoute() != null ? s.getRoute().getRouteNumber() : null,
                s.getPopularityScore(), sortScore);
    }
}
