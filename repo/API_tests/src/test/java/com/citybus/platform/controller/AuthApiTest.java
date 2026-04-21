package com.citybus.platform.controller;

import com.citybus.platform.dto.request.LoginRequest;
import com.citybus.platform.dto.request.RegisterRequest;
import com.citybus.platform.dto.response.LoginResponse;
import com.citybus.platform.entity.User;
import com.citybus.platform.exception.BusinessException;
import com.citybus.platform.exception.GlobalExceptionHandler;
import com.citybus.platform.security.JwtTokenProvider;
import com.citybus.platform.service.AuthService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class})
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class AuthApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthService authService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    void register_validRequest_returns200WithToken() throws Exception {
        LoginResponse resp = new LoginResponse("jwt", 1L, "alice", "PASSENGER", "Alice");
        when(authService.register(any(RegisterRequest.class))).thenReturn(resp);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setPassword("password123");
        req.setDisplayName("Alice");

        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt"))
                .andExpect(jsonPath("$.data.role").value("PASSENGER"));
    }

    @Test
    void register_blankUsername_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("");
        req.setPassword("password123");

        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setPassword("abc");

        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicateUsername_returnsBusinessError() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new BusinessException("Username already taken"));

        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setPassword("password123");

        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        LoginResponse resp = new LoginResponse("jwt", 1L, "alice", "PASSENGER", "Alice");
        when(authService.login(any(LoginRequest.class))).thenReturn(resp);

        LoginRequest req = new LoginRequest("alice", "password123");

        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt"))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    void login_blankUsername_returns400() throws Exception {
        LoginRequest req = new LoginRequest("", "password123");

        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void me_withAuthenticatedUser_returnsProfile() throws Exception {
        User user = TestAuth.user(1L, "alice", User.UserRole.PASSENGER);
        when(authService.getCurrentUser("alice")).thenReturn(user);

        mvc.perform(get("/api/auth/me").with(TestAuth.asPassenger()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    void me_unauthenticated_denied() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().is4xxClientError());
    }
}
