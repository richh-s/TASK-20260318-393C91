package com.citybus.platform.scheduler;

import com.citybus.platform.entity.*;
import com.citybus.platform.repository.NotificationPreferenceRepository;
import com.citybus.platform.repository.ReservationRepository;
import com.citybus.platform.service.MessageQueueService;
import com.citybus.platform.service.NotificationService;
import com.citybus.platform.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @Mock ReservationRepository reservationRepository;
    @Mock NotificationPreferenceRepository prefRepository;
    @Mock NotificationService notificationService;
    @Mock WorkflowService workflowService;
    @Mock MessageQueueService messageQueueService;

    @InjectMocks NotificationScheduler scheduler;

    private User user;
    private BusRoute route;
    private BusStop stop;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "defaultReminderMinutes", 10);
        ReflectionTestUtils.setField(scheduler, "missedCheckinMinutes", 5);

        user = new User();
        user.setId(1L);

        route = new BusRoute();
        route.setId(10L);
        route.setRouteNumber("101");

        stop = new BusStop();
        stop.setId(20L);
        stop.setNameEn("Zhongshan Road");
        stop.setRoute(route);
    }

    @Test
    void sendArrivalReminders_insideReminderWindow_sendsReminder() {
        Reservation res = buildReservation(LocalDateTime.now().plusMinutes(5));
        when(reservationRepository.findConfirmedBetween(any(), any())).thenReturn(List.of(res));
        when(prefRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        scheduler.sendArrivalReminders();

        verify(notificationService).createNotification(eq(1L),
                eq(Notification.NotificationType.ARRIVAL_REMINDER), anyString(), anyString(), eq(res.getId()));
    }

    @Test
    void sendArrivalReminders_outsideReminderWindow_doesNotSend() {
        Reservation res = buildReservation(LocalDateTime.now().plusMinutes(15));
        when(reservationRepository.findConfirmedBetween(any(), any())).thenReturn(List.of(res));
        when(prefRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        scheduler.sendArrivalReminders();

        verifyNoInteractions(notificationService);
    }

    @Test
    void sendArrivalReminders_dndActive_suppresses() {
        Reservation res = buildReservation(LocalDateTime.now().plusMinutes(5));

        NotificationPreference pref = new NotificationPreference();
        pref.setUser(user);
        pref.setRoute(route);
        pref.setStop(stop);
        pref.setEnabled(true);
        pref.setDndEnabled(true);
        pref.setDndStart(LocalTime.now().minusHours(1));
        pref.setDndEnd(LocalTime.now().plusHours(1));
        pref.setReminderMinutes(10);

        when(reservationRepository.findConfirmedBetween(any(), any())).thenReturn(List.of(res));
        when(prefRepository.findByUserId(1L)).thenReturn(List.of(pref));

        scheduler.sendArrivalReminders();

        verifyNoInteractions(notificationService);
    }

    @Test
    void detectMissedCheckins_overdueReservations_markedMissed() {
        Reservation res = buildReservation(LocalDateTime.now().minusMinutes(10));
        res.setId(50L);
        when(reservationRepository.findOverdueReservations(any())).thenReturn(List.of(res));

        scheduler.detectMissedCheckins();

        assertThat(res.getStatus()).isEqualTo(Reservation.ReservationStatus.MISSED);
        verify(reservationRepository).save(res);
        verify(notificationService).createNotification(eq(1L),
                eq(Notification.NotificationType.MISSED_CHECKIN), anyString(), anyString(), eq(50L));
    }

    @Test
    void escalateOverdueTasks_delegatesToWorkflowService() {
        when(workflowService.escalateOverdueTasks(any())).thenReturn(3);
        scheduler.escalateOverdueTasks();
        verify(workflowService).escalateOverdueTasks(any(LocalDateTime.class));
    }

    @Test
    void processMessageQueue_delegatesToMessageQueueService() {
        scheduler.processMessageQueue();
        verify(messageQueueService).processPending();
    }

    private Reservation buildReservation(LocalDateTime scheduledTime) {
        Reservation res = new Reservation();
        res.setId(100L);
        res.setUser(user);
        res.setRoute(route);
        res.setStop(stop);
        res.setScheduledTime(scheduledTime);
        res.setStatus(Reservation.ReservationStatus.CONFIRMED);
        return res;
    }
}
