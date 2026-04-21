package com.citybus.platform.service;

import com.citybus.platform.dto.response.RouteSearchResult;
import com.citybus.platform.dto.response.StopSearchResult;
import com.citybus.platform.entity.*;
import com.citybus.platform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock BusRouteRepository routeRepository;
    @Mock BusStopRepository stopRepository;
    @Mock SortingWeightRepository weightRepository;

    @InjectMocks SearchService searchService;

    private BusRoute route;
    private BusStop stop;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(searchService, "maxResults", 50);
        ReflectionTestUtils.setField(searchService, "autocompleteMax", 10);

        route = new BusRoute();
        route.setId(1L);
        route.setRouteNumber("101");
        route.setName("City Loop");
        route.setStatus("ACTIVE");

        stop = new BusStop();
        stop.setId(5L);
        stop.setNameEn("Zhongshan Road");
        stop.setNameCn("中山路");
        stop.setRoute(route);
        stop.setSequenceNumber(1);
        stop.setPopularityScore(80);
    }

    @Test
    void searchRoutes_matchingQuery_returnsResults() {
        when(routeRepository.searchRoutes("city")).thenReturn(List.of(route));

        List<RouteSearchResult> results = searchService.searchRoutes("city");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRouteNumber()).isEqualTo("101");
    }

    @Test
    void searchRoutes_blankQuery_returnsEmpty() {
        List<RouteSearchResult> results = searchService.searchRoutes("  ");
        assertThat(results).isEmpty();
        verifyNoInteractions(routeRepository);
    }

    @Test
    void searchRoutes_nullQuery_returnsEmpty() {
        List<RouteSearchResult> results = searchService.searchRoutes(null);
        assertThat(results).isEmpty();
    }

    @Test
    void searchStops_withoutRouteId_returnsWeightedSortedResults() {
        SortingWeight popWeight = new SortingWeight();
        popWeight.setFactorName("popularity_score");
        popWeight.setWeight(BigDecimal.valueOf(2.0));

        when(stopRepository.searchStops("zhong")).thenReturn(List.of(stop));
        when(weightRepository.findByFactorName("popularity_score")).thenReturn(Optional.of(popWeight));
        when(weightRepository.findByFactorName("frequency_score")).thenReturn(Optional.empty());

        List<StopSearchResult> results = searchService.searchStops("zhong", null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getNameEn()).isEqualTo("Zhongshan Road");
        assertThat(results.get(0).getSortScore()).isEqualTo(80 * 2.0);
    }

    @Test
    void searchStops_withRouteId_usesRouteScopedQuery() {
        when(stopRepository.searchStopsInRoute("zhong", 1L)).thenReturn(List.of(stop));
        when(weightRepository.findByFactorName(anyString())).thenReturn(Optional.empty());

        List<StopSearchResult> results = searchService.searchStops("zhong", 1L);

        assertThat(results).hasSize(1);
        verify(stopRepository).searchStopsInRoute("zhong", 1L);
        verify(stopRepository, never()).searchStops(anyString());
    }

    @Test
    void searchStops_noWeightConfigured_useDefaultWeight1() {
        when(stopRepository.searchStops("zhong")).thenReturn(List.of(stop));
        when(weightRepository.findByFactorName(anyString())).thenReturn(Optional.empty());

        List<StopSearchResult> results = searchService.searchStops("zhong", null);

        assertThat(results.get(0).getSortScore()).isEqualTo(80.0);
    }

    @Test
    void autocomplete_returnsStopNamesAndRouteLabels() {
        when(stopRepository.autocompleteNames("zh")).thenReturn(List.of("Zhongshan Road", "Zhongyuan St"));
        when(routeRepository.searchRoutes("zh")).thenReturn(List.of(route));

        List<String> suggestions = searchService.autocomplete("zh");

        assertThat(suggestions).contains("Zhongshan Road");
        assertThat(suggestions).anyMatch(s -> s.contains("101"));
    }

    @Test
    void autocomplete_blankQuery_returnsEmpty() {
        List<String> suggestions = searchService.autocomplete("");
        assertThat(suggestions).isEmpty();
    }
}
