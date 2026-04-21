package com.citybus.platform.controller;

import com.citybus.platform.dto.request.ReservationRequest;
import com.citybus.platform.dto.response.ReservationResponse;
import com.citybus.platform.exception.GlobalExceptionHandler;
import com.citybus.platform.security.JwtTokenProvider;
import com.citybus.platform.service.ReservationService;
import com.citybus.platform.service.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReservationController.class, excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class})
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class ReservationApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ReservationService reservationService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    void create_validRequest_returns200() throws Exception {
        ReservationResponse resp = new ReservationResponse(
                1L, 10L, "101", "Loop", 20L, "Zhongshan Road", "中山路",
                LocalDateTime.now().plusDays(1), "CONFIRMED", LocalDateTime.now());
        when(reservationService.create(eq(1L), any(ReservationRequest.class))).thenReturn(resp);

        ReservationRequest req = new ReservationRequest();
        req.setRouteId(10L);
        req.setStopId(20L);
        req.setScheduledTime(LocalDateTime.now().plusDays(1));

        mvc.perform(post("/api/reservations").with(csrf()).with(TestAuth.asPassenger())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.routeNumber").value("101"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void create_nullFields_returns400() throws Exception {
        ReservationRequest req = new ReservationRequest();

        mvc.perform(post("/api/reservations").with(csrf()).with(TestAuth.asPassenger())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_unauthenticated_denied() throws Exception {
        ReservationRequest req = new ReservationRequest();
        req.setRouteId(10L);
        req.setStopId(20L);
        req.setScheduledTime(LocalDateTime.now().plusDays(1));

        mvc.perform(post("/api/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void list_returnsPagedReservations() throws Exception {
        ReservationResponse r = new ReservationResponse(
                1L, 10L, "101", "Loop", 20L, "Stop", "中山路",
                LocalDateTime.now().plusDays(1), "CONFIRMED", LocalDateTime.now());
        Page<ReservationResponse> page = new PageImpl<>(List.of(r));
        when(reservationService.getMyReservations(eq(1L), any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/reservations").with(TestAuth.asPassenger()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].routeNumber").value("101"));
    }

    @Test
    void cancel_validRequest_returns200() throws Exception {
        ReservationResponse resp = new ReservationResponse(
                1L, 10L, "101", "Loop", 20L, "Stop", "中山路",
                LocalDateTime.now().plusDays(1), "CANCELLED", LocalDateTime.now());
        when(reservationService.cancel(1L, 1L)).thenReturn(resp);

        mvc.perform(delete("/api/reservations/1").with(csrf()).with(TestAuth.asPassenger()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
