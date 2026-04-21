package com.citybus.platform.service;

import com.citybus.platform.dto.request.LoginRequest;
import com.citybus.platform.dto.request.RegisterRequest;
import com.citybus.platform.dto.response.LoginResponse;
import com.citybus.platform.entity.User;
import com.citybus.platform.exception.BusinessException;
import com.citybus.platform.repository.UserRepository;
import com.citybus.platform.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;

    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        User user = (User) auth.getPrincipal();
        String token = tokenProvider.generateToken(auth);
        log.info("User logged in: {}", user.getUsername());
        auditService.log("LOGIN", "USER", user.getId(), user.getId(), null);
        return new LoginResponse(token, user.getId(), user.getUsername(),
                user.getRole().name(), user.getDisplayName());
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already taken");
        }
        if (request.getPassword().length() < 8) {
            throw new BusinessException("Password must be at least 8 characters");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName() != null ?
                request.getDisplayName() : request.getUsername());
        user.setRole(User.UserRole.PASSENGER);
        userRepository.save(user);

        String token = tokenProvider.generateToken(user.getUsername());
        log.info("New passenger registered: {}", user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername(),
                user.getRole().name(), user.getDisplayName());
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
