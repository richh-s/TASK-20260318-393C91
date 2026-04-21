package com.citybus.platform.service;

import com.citybus.platform.dto.request.NotificationPrefRequest;
import com.citybus.platform.dto.response.NotificationResponse;
import com.citybus.platform.entity.*;
import com.citybus.platform.exception.BusinessException;
import com.citybus.platform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationPreferenceRepository prefRepository;
    @Mock NotificationTemplateRepository templateRepository;
    @Mock UserRepository userRepository;
    @Mock BusRouteRepository routeRepository;
    @Mock BusStopRepository stopRepository;

    @InjectMocks NotificationService notificationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
    }

    @Test
    void createNotification_savesAndReturns() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any())).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(5L);
            return n;
        });

        Notification result = notificationService.createNotification(1L,
                Notification.NotificationType.ARRIVAL_REMINDER,
                "Title", "Content", 10L);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getType()).isEqualTo(Notification.NotificationType.ARRIVAL_REMINDER);
        assertThat(result.getContent()).isEqualTo("Content");
    }

    @Test
    void createNotification_taskEscalated_masksPhoneNumbers() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Notification result = notificationService.createNotification(1L,
                Notification.NotificationType.TASK_ESCALATED,
                "Escalated", "Contact 13912345678 for details", 1L);

        assertThat(result.getContent()).doesNotContain("13912345678");
        assertThat(result.getContent()).contains("***");
    }

    @Test
    void createNotification_arrivalReminder_doesNotMaskContent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Notification result = notificationService.createNotification(1L,
                Notification.NotificationType.ARRIVAL_REMINDER,
                "Title", "Bus 101 at Zhongshan Road", 1L);

        assertThat(result.getContent()).isEqualTo("Bus 101 at Zhongshan Road");
    }

    @Test
    void markRead_ownerMarksRead_succeeds() {
        Notification notification = new Notification();
        notification.setId(5L);
        notification.setUser(user);
        notification.setRead(false);

        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenReturn(notification);

        notificationService.markRead(5L, 1L);
        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void markRead_wrongOwner_throwsForbidden() {
        User other = new User();
        other.setId(99L);
        Notification notification = new Notification();
        notification.setId(5L);
        notification.setUser(other);

        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));
        assertThatThrownBy(() -> notificationService.markRead(5L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void markAllRead_callsRepository() {
        notificationService.markAllRead(1L);
        verify(notificationRepository).markAllReadByUserId(1L);
    }

    @Test
    void getUnreadCount_returnsCount() {
        when(notificationRepository.countByUserIdAndReadFalse(1L)).thenReturn(3L);
        assertThat(notificationService.getUnreadCount(1L)).isEqualTo(3L);
    }

    @Test
    void savePreference_newPref_savesWithParsedDndTimes() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(prefRepository.findByUserIdAndRouteIdAndStopId(1L, null, null))
                .thenReturn(Optional.empty());
        when(prefRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationPrefRequest req = new NotificationPrefRequest();
        req.setReminderMinutes(15);
        req.setEnabled(true);
        req.setDndEnabled(true);
        req.setDndStart("22:00");
        req.setDndEnd("07:00");

        NotificationPreference result = notificationService.savePreference(1L, req);

        assertThat(result.isEnabled()).isTrue();
        assertThat(result.isDndEnabled()).isTrue();
        assertThat(result.getDndStart()).isNotNull();
        assertThat(result.getDndEnd()).isNotNull();
    }

    @Test
    void getPreferences_returnsUserPrefs() {
        NotificationPreference pref = new NotificationPreference();
        pref.setId(1L);
        when(prefRepository.findByUserId(1L)).thenReturn(List.of(pref));

        List<NotificationPreference> result = notificationService.getPreferences(1L);
        assertThat(result).hasSize(1);
    }

    @Test
    void getNotifications_returnsMappedPage() {
        Notification n = new Notification();
        n.setId(1L);
        n.setUser(user);
        n.setType(Notification.NotificationType.ARRIVAL_REMINDER);
        n.setTitle("Title");
        n.setContent("Content");
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());

        Page<Notification> page = new PageImpl<>(List.of(n));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        Page<NotificationResponse> result = notificationService.getNotifications(1L, PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo("ARRIVAL_REMINDER");
    }
}
