package com.citybus.platform.service;

import com.citybus.platform.dto.request.LoginRequest;
import com.citybus.platform.dto.request.RegisterRequest;
import com.citybus.platform.dto.response.LoginResponse;
import com.citybus.platform.entity.User;
import com.citybus.platform.exception.BusinessException;
import com.citybus.platform.repository.UserRepository;
import com.citybus.platform.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtTokenProvider tokenProvider;
    @Mock AuditService auditService;

    @InjectMocks AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setPassword("encoded_pass");
        user.setRole(User.UserRole.PASSENGER);
        user.setDisplayName("Alice");
    }

    @Test
    void login_validCredentials_returnsToken() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenProvider.generateToken(auth)).thenReturn("jwt-token");

        LoginResponse resp = authService.login(new LoginRequest("alice", "password123"));

        assertThat(resp.getToken()).isEqualTo("jwt-token");
        assertThat(resp.getUsername()).isEqualTo("alice");
        assertThat(resp.getRole()).isEqualTo("PASSENGER");
        verify(auditService).log(eq("LOGIN"), eq("USER"), eq(1L), eq(1L), isNull());
    }

    @Test
    void login_badCredentials_throws() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void register_newUser_returnsToken() {
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });
        when(tokenProvider.generateToken("bob")).thenReturn("new-token");

        RegisterRequest req = new RegisterRequest();
        req.setUsername("bob");
        req.setPassword("password123");
        req.setDisplayName("Bob");

        LoginResponse resp = authService.register(req);

        assertThat(resp.getToken()).isEqualTo("new-token");
        assertThat(resp.getUsername()).isEqualTo("bob");
        assertThat(resp.getRole()).isEqualTo("PASSENGER");
    }

    @Test
    void register_duplicateUsername_throwsBusinessException() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setPassword("password123");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void register_shortPassword_throwsBusinessException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("short");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("8 characters");
    }

    @Test
    void register_noDisplayName_usesUsername() {
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(3L);
            return u;
        });
        when(tokenProvider.generateToken(anyString())).thenReturn("tok");

        RegisterRequest req = new RegisterRequest();
        req.setUsername("bob");
        req.setPassword("password123");

        LoginResponse resp = authService.register(req);
        assertThat(resp.getDisplayName()).isEqualTo("bob");
    }

    @Test
    void getCurrentUser_existingUser_returnsUser() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        User result = authService.getCurrentUser("alice");
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getCurrentUser_missingUser_throwsNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.getCurrentUser("ghost"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found");
    }
}
