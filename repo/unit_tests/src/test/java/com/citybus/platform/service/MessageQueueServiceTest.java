package com.citybus.platform.service;

import com.citybus.platform.entity.MessageQueueItem;
import com.citybus.platform.entity.Notification;
import com.citybus.platform.repository.MessageQueueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageQueueServiceTest {

    @Mock MessageQueueRepository queueRepository;
    @Mock NotificationService notificationService;

    @InjectMocks MessageQueueService messageQueueService;

    @Test
    void enqueue_savesPendingItem() {
        when(queueRepository.save(any())).thenAnswer(inv -> {
            MessageQueueItem i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });

        Map<String, Object> payload = Map.of("userId", 1L);
        MessageQueueItem item = messageQueueService.enqueue("NOTIFICATION", payload, null);

        assertThat(item.getStatus()).isEqualTo(MessageQueueItem.QueueStatus.PENDING);
        assertThat(item.getType()).isEqualTo("NOTIFICATION");
        assertThat(item.getScheduledAt()).isNotNull();
    }

    @Test
    void processPending_notificationType_dispatchesToNotificationService() {
        MessageQueueItem item = buildItem("NOTIFICATION", Map.of(
                "userId", 1L,
                "notificationType", "ARRIVAL_REMINDER",
                "title", "Title",
                "content", "Content",
                "entityId", 2L
        ));
        when(queueRepository.findPendingMessages(any())).thenReturn(List.of(item));
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        messageQueueService.processPending();

        verify(notificationService).createNotification(eq(1L),
                eq(Notification.NotificationType.ARRIVAL_REMINDER), eq("Title"), eq("Content"), eq(2L));
        assertThat(item.getStatus()).isEqualTo(MessageQueueItem.QueueStatus.DONE);
    }

    @Test
    void processPending_failureUnderMaxRetries_reschedulesWithBackoff() {
        MessageQueueItem item = buildItem("NOTIFICATION", Map.of(
                "userId", 1L, "notificationType", "ARRIVAL_REMINDER",
                "title", "T", "content", "C", "entityId", 2L));
        item.setRetryCount(0);

        when(queueRepository.findPendingMessages(any())).thenReturn(List.of(item));
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("simulated")).when(notificationService)
                .createNotification(any(), any(), any(), any(), any());

        messageQueueService.processPending();

        assertThat(item.getRetryCount()).isEqualTo(1);
        assertThat(item.getStatus()).isEqualTo(MessageQueueItem.QueueStatus.PENDING);
        assertThat(item.getError()).contains("simulated");
    }

    @Test
    void processPending_failureAtMaxRetries_marksFailed() {
        MessageQueueItem item = buildItem("NOTIFICATION", Map.of(
                "userId", 1L, "notificationType", "ARRIVAL_REMINDER",
                "title", "T", "content", "C", "entityId", 2L));
        item.setRetryCount(2);

        when(queueRepository.findPendingMessages(any())).thenReturn(List.of(item));
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("bad")).when(notificationService)
                .createNotification(any(), any(), any(), any(), any());

        messageQueueService.processPending();

        assertThat(item.getStatus()).isEqualTo(MessageQueueItem.QueueStatus.FAILED);
        assertThat(item.getRetryCount()).isEqualTo(3);
    }

    @Test
    void processPending_unknownType_logsWarningButStillMarksDone() {
        MessageQueueItem item = buildItem("UNKNOWN_TYPE", new HashMap<>());
        when(queueRepository.findPendingMessages(any())).thenReturn(List.of(item));
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        messageQueueService.processPending();

        assertThat(item.getStatus()).isEqualTo(MessageQueueItem.QueueStatus.DONE);
        verifyNoInteractions(notificationService);
    }

    @Test
    void getPendingCount_returnsRepositoryCount() {
        when(queueRepository.countByStatus(MessageQueueItem.QueueStatus.PENDING)).thenReturn(7L);
        assertThat(messageQueueService.getPendingCount()).isEqualTo(7L);
    }

    private MessageQueueItem buildItem(String type, Map<String, Object> payload) {
        MessageQueueItem item = new MessageQueueItem();
        item.setId(1L);
        item.setType(type);
        item.setPayload(payload);
        item.setStatus(MessageQueueItem.QueueStatus.PENDING);
        item.setScheduledAt(LocalDateTime.now());
        return item;
    }
}
