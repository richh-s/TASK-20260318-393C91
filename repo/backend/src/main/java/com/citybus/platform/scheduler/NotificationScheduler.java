package com.citybus.platform.scheduler;

import com.citybus.platform.entity.Notification;
import com.citybus.platform.entity.NotificationPreference;
import com.citybus.platform.entity.Reservation;
import com.citybus.platform.repository.NotificationPreferenceRepository;
import com.citybus.platform.repository.ReservationRepository;
import com.citybus.platform.service.MessageQueueService;
import com.citybus.platform.service.NotificationService;
import com.citybus.platform.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final ReservationRepository reservationRepository;
    private final NotificationPreferenceRepository prefRepository;
    private final NotificationService notificationService;
    private final WorkflowService workflowService;
    private final MessageQueueService messageQueueService;

    @Value("${app.notification.default-reminder-minutes:10}")
    private int defaultReminderMinutes;

    @Value("${app.notification.missed-checkin-minutes:5}")
    private int missedCheckinMinutes;

    @Scheduled(fixedDelayString = "${app.scheduler.reminder-interval-ms:60000}")
    public void sendArrivalReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusMinutes(defaultReminderMinutes + 2);
        List<Reservation> upcoming = reservationRepository.findConfirmedBetween(now, windowEnd);

        for (Reservation res : upcoming) {
            Long userId = res.getUser().getId();
            List<NotificationPreference> prefs = prefRepository.findByUserId(userId);
            int reminderMin = prefs.stream()
                    .filter(p -> matchesReservation(p, res))
                    .mapToInt(NotificationPreference::getReminderMinutes)
                    .findFirst().orElse(defaultReminderMinutes);

            LocalDateTime reminderTime = res.getScheduledTime().minusMinutes(reminderMin);
            if (now.isAfter(reminderTime) || now.isEqual(reminderTime)) {
                if (shouldSendForPrefs(prefs, res, now)) {
                    notificationService.createNotification(userId,
                            Notification.NotificationType.ARRIVAL_REMINDER,
                            "Arrival Reminder",
                            "Your bus " + res.getRoute().getRouteNumber() + " departs in "
                                    + reminderMin + " minutes from " + res.getStop().getNameEn(),
                            res.getId());
                    log.debug("Arrival reminder sent for reservation {}", res.getId());
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.scheduler.missed-checkin-interval-ms:120000}")
    public void detectMissedCheckins() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(missedCheckinMinutes);
        List<Reservation> overdue = reservationRepository.findOverdueReservations(threshold);
        for (Reservation res : overdue) {
            res.setStatus(com.citybus.platform.entity.Reservation.ReservationStatus.MISSED);
            reservationRepository.save(res);
            notificationService.createNotification(res.getUser().getId(),
                    Notification.NotificationType.MISSED_CHECKIN,
                    "Missed Check-in",
                    "You missed your scheduled bus " + res.getRoute().getRouteNumber()
                            + " at " + res.getStop().getNameEn(),
                    res.getId());
            log.debug("Missed check-in detected for reservation {}", res.getId());
        }
    }

    @Scheduled(cron = "${app.scheduler.escalation-cron:0 0 * * * *}")
    public void escalateOverdueTasks() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        int count = workflowService.escalateOverdueTasks(threshold);
        if (count > 0) {
            log.info("Escalated {} overdue workflow tasks", count);
        }
    }

    @Scheduled(fixedDelayString = "${app.scheduler.queue-interval-ms:30000}")
    public void processMessageQueue() {
        messageQueueService.processPending();
    }

    private boolean matchesReservation(NotificationPreference pref, Reservation res) {
        boolean routeMatch = pref.getRoute() == null || pref.getRoute().getId().equals(res.getRoute().getId());
        boolean stopMatch = pref.getStop() == null || pref.getStop().getId().equals(res.getStop().getId());
        return routeMatch && stopMatch && pref.isEnabled();
    }

    private boolean shouldSendForPrefs(List<NotificationPreference> prefs, Reservation res, LocalDateTime now) {
        LocalTime currentTime = now.toLocalTime();
        return prefs.stream()
                .filter(p -> matchesReservation(p, res))
                .noneMatch(p -> p.isDndEnabled()
                        && p.getDndStart() != null && p.getDndEnd() != null
                        && isInDnd(currentTime, p.getDndStart(), p.getDndEnd()));
    }

    private boolean isInDnd(LocalTime current, LocalTime start, LocalTime end) {
        if (start.isBefore(end)) {
            return current.isAfter(start) && current.isBefore(end);
        }
        return current.isAfter(start) || current.isBefore(end);
    }
}
