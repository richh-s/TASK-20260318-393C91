package com.citybus.platform.controller;

import com.citybus.platform.dto.response.ImportResponse;
import com.citybus.platform.exception.GlobalExceptionHandler;
import com.citybus.platform.security.JwtTokenProvider;
import com.citybus.platform.service.BusDataService;
import com.citybus.platform.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BusDataController.class, excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class})
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class BusDataApiTest {

    @Autowired MockMvc mvc;
    @MockBean BusDataService busDataService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    void listImports_asAdmin_returnsPage() throws Exception {
        ImportResponse r = new ImportResponse(
                1L, "JSON", "DONE", "routes.json", 50, 0, LocalDateTime.now());
        Page<ImportResponse> page = new PageImpl<>(List.of(r));
        when(busDataService.listImports(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/bus-data/imports").with(TestAuth.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].fileName").value("routes.json"));
    }

    @Test
    void listImports_asPassenger_forbidden() throws Exception {
        mvc.perform(get("/api/bus-data/imports").with(TestAuth.asPassenger()))
                .andExpect(status().isForbidden());
    }

    @Test
    void upload_validFile_returns200() throws Exception {
        ImportResponse r = new ImportResponse(
                1L, "JSON", "PENDING", "data.json", 0, 0, LocalDateTime.now());
        when(busDataService.upload(any(), eq("JSON"), eq(3L))).thenReturn(r);

        MockMultipartFile file = new MockMultipartFile(
                "file", "data.json", "application/json", "[]".getBytes());

        mvc.perform(multipart("/api/bus-data/imports")
                        .file(file)
                        .param("importType", "JSON")
                        .with(csrf()).with(TestAuth.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void upload_asPassenger_forbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "data.json", "application/json", "[]".getBytes());

        mvc.perform(multipart("/api/bus-data/imports")
                        .file(file)
                        .with(csrf()).with(TestAuth.asPassenger()))
                .andExpect(status().isForbidden());
    }
}
