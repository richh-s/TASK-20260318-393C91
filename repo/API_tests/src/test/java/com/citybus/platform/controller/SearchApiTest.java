package com.citybus.platform.controller;

import com.citybus.platform.dto.response.RouteSearchResult;
import com.citybus.platform.dto.response.StopSearchResult;
import com.citybus.platform.exception.GlobalExceptionHandler;
import com.citybus.platform.security.JwtTokenProvider;
import com.citybus.platform.service.SearchService;
import com.citybus.platform.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SearchController.class, excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class})
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class SearchApiTest {

    @Autowired MockMvc mvc;
    @MockBean SearchService searchService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    void searchRoutes_returnsResults() throws Exception {
        when(searchService.searchRoutes("city")).thenReturn(List.of(
                new RouteSearchResult(1L, "101", "City Loop", "Downtown", null, 10)));

        mvc.perform(get("/api/search/routes").param("q", "city"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].routeNumber").value("101"))
                .andExpect(jsonPath("$.data[0].stopCount").value(10));
    }

    @Test
    void searchRoutes_missingQueryParam_returns400() throws Exception {
        mvc.perform(get("/api/search/routes"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchStops_withQueryAndRouteId_returnsResults() throws Exception {
        when(searchService.searchStops("zhong", 1L)).thenReturn(List.of(
                new StopSearchResult(5L, "Zhongshan Road", "中山路", "addr", 1, 1L, "101", 80, 80.0)));

        mvc.perform(get("/api/search/stops").param("q", "zhong").param("routeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nameEn").value("Zhongshan Road"))
                .andExpect(jsonPath("$.data[0].popularityScore").value(80));
    }

    @Test
    void searchStops_withoutRouteId_returnsResults() throws Exception {
        when(searchService.searchStops(eq("zhong"), isNull())).thenReturn(List.of());

        mvc.perform(get("/api/search/stops").param("q", "zhong"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void autocomplete_returnsSuggestions() throws Exception {
        when(searchService.autocomplete("zh")).thenReturn(List.of("Zhongshan Road", "101 - City Loop"));

        mvc.perform(get("/api/search/autocomplete").param("q", "zh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("Zhongshan Road"));
    }
}
