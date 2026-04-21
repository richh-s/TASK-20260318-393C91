package com.citybus.platform.controller;

import com.citybus.platform.common.ApiResponse;
import com.citybus.platform.dto.request.NotificationPrefRequest;
import com.citybus.platform.entity.User;
import com.citybus.platform.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(@AuthenticationPrincipal User user,
                                                @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.getNotifications(user.getId(), pageable)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<?>> unreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getUnreadCount(user.getId())));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<?>> markRead(@AuthenticationPrincipal User user,
                                                    @PathVariable Long id) {
        notificationService.markRead(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<?>> markAllRead(@AuthenticationPrincipal User user) {
        notificationService.markAllRead(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<?>> getPrefs(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getPreferences(user.getId())));
    }

    @PostMapping("/preferences")
    public ResponseEntity<ApiResponse<?>> savePrefs(@AuthenticationPrincipal User user,
                                                     @Valid @RequestBody NotificationPrefRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.savePreference(user.getId(), req)));
    }
}
