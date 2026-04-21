package com.citybus.platform.controller;

import com.citybus.platform.dto.request.NotificationPrefRequest;
import com.citybus.platform.dto.response.NotificationResponse;
import com.citybus.platform.entity.NotificationPreference;
import com.citybus.platform.exception.GlobalExceptionHandler;
import com.citybus.platform.security.JwtTokenProvider;
import com.citybus.platform.service.NotificationService;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NotificationController.class, excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class})
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class NotificationApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean NotificationService notificationService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    void list_returnsPagedNotifications() throws Exception {
        NotificationResponse n = new NotificationResponse(
                1L, "ARRIVAL_REMINDER", "Title", "Content", false, 10L, LocalDateTime.now());
        Page<NotificationResponse> page = new PageImpl<>(List.of(n));
        when(notificationService.getNotifications(eq(1L), any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/notifications").with(TestAuth.asPassenger()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("ARRIVAL_REMINDER"));
    }

    @Test
    void list_unauthenticated_denied() throws Exception {
        mvc.perform(get("/api/notifications"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void unreadCount_returnsCount() throws Exception {
        when(notificationService.getUnreadCount(1L)).thenReturn(5L);

        mvc.perform(get("/api/notifications/unread-count").with(TestAuth.asPassenger()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    void markRead_validId_returns200() throws Exception {
        mvc.perform(patch("/api/notifications/1/read").with(csrf()).with(TestAuth.asPassenger()))
                .andExpect(status().isOk());
        verify(notificationService).markRead(1L, 1L);
    }

    @Test
    void markAllRead_returns200() throws Exception {
        mvc.perform(patch("/api/notifications/read-all").with(csrf()).with(TestAuth.asPassenger()))
                .andExpect(status().isOk());
        verify(notificationService).markAllRead(1L);
    }

    @Test
    void getPrefs_returnsList() throws Exception {
        NotificationPreference pref = new NotificationPreference();
        pref.setId(1L);
        pref.setReminderMinutes(10);
        pref.setEnabled(true);
        when(notificationService.getPreferences(1L)).thenReturn(List.of(pref));

        mvc.perform(get("/api/notifications/preferences").with(TestAuth.asPassenger()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].reminderMinutes").value(10));
    }

    @Test
    void savePrefs_validRequest_returns200() throws Exception {
        NotificationPreference pref = new NotificationPreference();
        pref.setId(1L);
        pref.setReminderMinutes(15);
        when(notificationService.savePreference(eq(1L), any(NotificationPrefRequest.class)))
                .thenReturn(pref);

        NotificationPrefRequest req = new NotificationPrefRequest();
        req.setRouteId(10L);
        req.setStopId(20L);
        req.setReminderMinutes(15);
        req.setEnabled(true);

        mvc.perform(post("/api/notifications/preferences")
                        .with(csrf()).with(TestAuth.asPassenger())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reminderMinutes").value(15));
    }
}
