package com.citybus.platform.controller;

import com.citybus.platform.entity.User;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class TestAuth {
    private TestAuth() {}

    public static User user(long id, String username, User.UserRole role) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setPassword("x");
        u.setRole(role);
        u.setDisplayName(username);
        u.setEnabled(true);
        return u;
    }

    public static RequestPostProcessor asPassenger() {
        return SecurityMockMvcRequestPostProcessors.user(user(1L, "alice", User.UserRole.PASSENGER));
    }

    public static RequestPostProcessor asDispatcher() {
        return SecurityMockMvcRequestPostProcessors.user(user(2L, "dispatch1", User.UserRole.DISPATCHER));
    }

    public static RequestPostProcessor asAdmin() {
        return SecurityMockMvcRequestPostProcessors.user(user(3L, "admin1", User.UserRole.ADMIN));
    }
}
