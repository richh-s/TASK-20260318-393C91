package com.citybus.platform.service;

import com.citybus.platform.entity.MessageQueueItem;
import com.citybus.platform.entity.Notification;
import com.citybus.platform.repository.MessageQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageQueueService {

    private final MessageQueueRepository queueRepository;
    private final NotificationService notificationService;

    private static final int MAX_RETRIES = 3;

    @Transactional
    public MessageQueueItem enqueue(String type, Map<String, Object> payload, LocalDateTime scheduledAt) {
        MessageQueueItem item = new MessageQueueItem();
        item.setType(type);
        item.setPayload(payload);
        item.setStatus(MessageQueueItem.QueueStatus.PENDING);
        item.setScheduledAt(scheduledAt != null ? scheduledAt : LocalDateTime.now());
        return queueRepository.save(item);
    }

    @Transactional
    public void processPending() {
        List<MessageQueueItem> pending = queueRepository.findPendingMessages(LocalDateTime.now());
        log.debug("Processing {} queued messages", pending.size());
        for (MessageQueueItem item : pending) {
            processItem(item);
        }
    }

    private void processItem(MessageQueueItem item) {
        item.setStatus(MessageQueueItem.QueueStatus.PROCESSING);
        queueRepository.save(item);
        try {
            dispatch(item);
            item.setStatus(MessageQueueItem.QueueStatus.DONE);
            item.setProcessedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Message {} failed (attempt {}): {}", item.getId(), item.getRetryCount() + 1, e.getMessage());
            item.setRetryCount(item.getRetryCount() + 1);
            item.setError(e.getMessage());
            if (item.getRetryCount() >= MAX_RETRIES) {
                item.setStatus(MessageQueueItem.QueueStatus.FAILED);
            } else {
                item.setStatus(MessageQueueItem.QueueStatus.PENDING);
                item.setScheduledAt(LocalDateTime.now().plusMinutes(5L * item.getRetryCount()));
            }
        }
        queueRepository.save(item);
    }

    private void dispatch(MessageQueueItem item) {
        Map<String, Object> payload = item.getPayload();
        switch (item.getType()) {
            case "NOTIFICATION" -> {
                Long userId = getLong(payload, "userId");
                String typeStr = getString(payload, "notificationType");
                String title = getString(payload, "title");
                String content = getString(payload, "content");
                Long entityId = getLong(payload, "entityId");
                notificationService.createNotification(userId,
                        Notification.NotificationType.valueOf(typeStr), title, content, entityId);
            }
            default -> log.warn("Unknown message type: {}", item.getType());
        }
    }

    public long getPendingCount() {
        return queueRepository.countByStatus(MessageQueueItem.QueueStatus.PENDING);
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.longValue();
        return val != null ? Long.parseLong(val.toString()) : null;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
