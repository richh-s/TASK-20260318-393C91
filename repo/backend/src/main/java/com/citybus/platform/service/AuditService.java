package com.citybus.platform.service;

import com.citybus.platform.entity.AuditLog;
import com.citybus.platform.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(String action, String entityType, Long entityId, Long userId,
                    Map<String, Object> details) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setUserId(userId);
        log.setTraceId(MDC.get("traceId"));
        log.setDetails(details);
        auditLogRepository.save(log);
    }
}
