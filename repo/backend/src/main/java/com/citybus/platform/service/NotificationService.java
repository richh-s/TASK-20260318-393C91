package com.citybus.platform.service;

import com.citybus.platform.dto.request.NotificationPrefRequest;
import com.citybus.platform.dto.response.NotificationResponse;
import com.citybus.platform.entity.*;
import com.citybus.platform.exception.BusinessException;
import com.citybus.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository prefRepository;
    private final NotificationTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final BusRouteRepository routeRepository;
    private final BusStopRepository stopRepository;

    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!notification.getUser().getId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Access denied");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    @Transactional
    public NotificationPreference savePreference(Long userId, NotificationPrefRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));

        BusRoute route = req.getRouteId() != null ?
                routeRepository.findById(req.getRouteId()).orElse(null) : null;
        BusStop stop = req.getStopId() != null ?
                stopRepository.findById(req.getStopId()).orElse(null) : null;

        NotificationPreference pref = prefRepository
                .findByUserIdAndRouteIdAndStopId(userId, req.getRouteId(), req.getStopId())
                .orElse(new NotificationPreference());

        pref.setUser(user);
        pref.setRoute(route);
        pref.setStop(stop);
        pref.setReminderMinutes(req.getReminderMinutes() != null ? req.getReminderMinutes() : 10);
        pref.setEnabled(req.isEnabled());
        pref.setDndEnabled(req.isDndEnabled());
        if (req.isDndEnabled()) {
            pref.setDndStart(req.getDndStart() != null ? LocalTime.parse(req.getDndStart()) : null);
            pref.setDndEnd(req.getDndEnd() != null ? LocalTime.parse(req.getDndEnd()) : null);
        }
        return prefRepository.save(pref);
    }

    public List<NotificationPreference> getPreferences(Long userId) {
        return prefRepository.findByUserId(userId);
    }

    @Transactional
    public Notification createNotification(Long userId, Notification.NotificationType type,
                                           String title, String content, Long entityId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));

        String maskedContent = maskSensitiveContent(content, type);

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(maskedContent);
        notification.setEntityId(entityId);
        log.info("Creating notification for user={} type={}", userId, type);
        return notificationRepository.save(notification);
    }

    private String maskSensitiveContent(String content, Notification.NotificationType type) {
        if (type == Notification.NotificationType.TASK_ESCALATED) {
            return content.replaceAll("\\b\\d{11}\\b", "***-****-****");
        }
        return content;
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType().name(), n.getTitle(),
                n.getContent(), n.isRead(), n.getEntityId(), n.getCreatedAt());
    }
}
