package com.citybus.platform.controller;

import com.citybus.platform.dto.request.*;
import com.citybus.platform.entity.*;
import com.citybus.platform.exception.GlobalExceptionHandler;
import com.citybus.platform.security.JwtTokenProvider;
import com.citybus.platform.service.AdminService;
import com.citybus.platform.service.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class, excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class})
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class AdminApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AdminService adminService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    void getTemplates_asAdmin_returnsList() throws Exception {
        NotificationTemplate t = new NotificationTemplate();
        t.setId(1L);
        t.setName("arrival");
        t.setType(Notification.NotificationType.ARRIVAL_REMINDER);
        when(adminService.getTemplates()).thenReturn(List.of(t));

        mvc.perform(get("/api/admin/templates").with(TestAuth.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("arrival"));
    }

    @Test
    void getTemplates_asPassenger_forbidden() throws Exception {
        mvc.perform(get("/api/admin/templates").with(TestAuth.asPassenger()))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveTemplate_validRequest_returns200() throws Exception {
        NotificationTemplate t = new NotificationTemplate();
        t.setId(1L);
        t.setName("arrival");
        when(adminService.saveTemplate(any(TemplateRequest.class))).thenReturn(t);

        TemplateRequest req = new TemplateRequest();
        req.setName("arrival");
        req.setType(Notification.NotificationType.ARRIVAL_REMINDER);
        req.setTitleTemplate("T");
        req.setContentTemplate("C");

        mvc.perform(post("/api/admin/templates").with(csrf()).with(TestAuth.asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("arrival"));
    }

    @Test
    void deleteTemplate_asAdmin_returns200() throws Exception {
        mvc.perform(delete("/api/admin/templates/1").with(csrf()).with(TestAuth.asAdmin()))
                .andExpect(status().isOk());
        verify(adminService).deleteTemplate(1L);
    }

    @Test
    void getWeights_asAdmin_returnsList() throws Exception {
        SortingWeight w = new SortingWeight();
        w.setId(1L);
        w.setFactorName("popularity_score");
        w.setWeight(BigDecimal.valueOf(1.5));
        when(adminService.getWeights()).thenReturn(List.of(w));

        mvc.perform(get("/api/admin/weights").with(TestAuth.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].factorName").value("popularity_score"));
    }

    @Test
    void saveWeight_validRequest_returns200() throws Exception {
        SortingWeight w = new SortingWeight();
        w.setId(1L);
        w.setFactorName("popularity_score");
        w.setWeight(BigDecimal.valueOf(2.0));
        when(adminService.saveWeight(any(SortingWeightRequest.class))).thenReturn(w);

        SortingWeightRequest req = new SortingWeightRequest();
        req.setFactorName("popularity_score");
        req.setWeight(BigDecimal.valueOf(2.0));

        mvc.perform(post("/api/admin/weights").with(csrf()).with(TestAuth.asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void getDictionaries_withFieldName_filters() throws Exception {
        FieldDictionary d = new FieldDictionary();
        d.setFieldName("stopStatus");
        when(adminService.getDictionaries("stopStatus")).thenReturn(List.of(d));

        mvc.perform(get("/api/admin/dictionaries").param("fieldName", "stopStatus")
                        .with(TestAuth.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].fieldName").value("stopStatus"));
    }

    @Test
    void saveDictionary_validRequest_returns200() throws Exception {
        FieldDictionary d = new FieldDictionary();
        d.setId(1L);
        d.setFieldName("stopStatus");
        d.setRawValue("运营");
        d.setStandardValue("ACTIVE");
        when(adminService.saveDictionary(any(FieldDictionaryRequest.class))).thenReturn(d);

        FieldDictionaryRequest req = new FieldDictionaryRequest();
        req.setFieldName("stopStatus");
        req.setRawValue("运营");
        req.setStandardValue("ACTIVE");

        mvc.perform(post("/api/admin/dictionaries").with(csrf()).with(TestAuth.asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteDictionary_asAdmin_returns200() throws Exception {
        mvc.perform(delete("/api/admin/dictionaries/1").with(csrf()).with(TestAuth.asAdmin()))
                .andExpect(status().isOk());
        verify(adminService).deleteDictionary(1L);
    }

    @Test
    void getConfigs_asAdmin_returnsList() throws Exception {
        SystemConfig c = new SystemConfig();
        c.setConfigKey("key");
        c.setConfigValue("value");
        when(adminService.getConfigs()).thenReturn(List.of(c));

        mvc.perform(get("/api/admin/configs").with(TestAuth.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].configKey").value("key"));
    }

    @Test
    void saveConfig_validRequest_returns200() throws Exception {
        SystemConfig c = new SystemConfig();
        c.setId(1L);
        c.setConfigKey("max");
        c.setConfigValue("10");
        when(adminService.saveConfig(any(SystemConfigRequest.class))).thenReturn(c);

        SystemConfigRequest req = new SystemConfigRequest();
        req.setConfigKey("max");
        req.setConfigValue("10");

        mvc.perform(post("/api/admin/configs").with(csrf()).with(TestAuth.asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
