package com.citybus.platform.service;

import com.citybus.platform.dto.request.WorkflowApprovalRequest;
import com.citybus.platform.dto.request.WorkflowTaskRequest;
import com.citybus.platform.dto.response.WorkflowApprovalResponse;
import com.citybus.platform.dto.response.WorkflowTaskResponse;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowService {

    private final WorkflowTaskRepository taskRepository;
    private final WorkflowApprovalRepository approvalRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final AtomicLong TASK_COUNTER = new AtomicLong(1000);

    @Transactional
    public WorkflowTaskResponse createTask(Long creatorId, WorkflowTaskRequest req) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));

        User assignedTo = null;
        if (req.getAssignedToId() != null) {
            assignedTo = userRepository.findById(req.getAssignedToId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Assigned user not found"));
        }

        WorkflowTask task = new WorkflowTask();
        task.setTaskNumber(generateTaskNumber(req.getType()));
        task.setType(req.getType());
        task.setTitle(req.getTitle());
        task.setDescription(req.getDescription());
        task.setStatus(WorkflowTask.TaskStatus.PENDING);
        task.setCreatedBy(creator);
        task.setAssignedTo(assignedTo);
        task.setDeadline(req.getDeadline());
        task.setPayload(req.getPayload());
        task.setEscalated(false);
        taskRepository.save(task);

        if (assignedTo != null) {
            notificationService.createNotification(assignedTo.getId(),
                    Notification.NotificationType.TASK_ASSIGNED,
                    "New Task Assigned: " + task.getTaskNumber(),
                    "You have been assigned task: " + task.getTitle(), task.getId());
        }

        log.info("Workflow task created: {} by user={}", task.getTaskNumber(), creatorId);
        return toResponse(task);
    }

    public Page<WorkflowTaskResponse> getMyTasks(Long userId, Pageable pageable) {
        return taskRepository.findByAssignedToIdOrderByCreatedAtDesc(userId, pageable).map(this::toResponse);
    }

    public Page<WorkflowTaskResponse> getTasksByStatus(WorkflowTask.TaskStatus status, Pageable pageable) {
        return taskRepository.findByStatusOrderByCreatedAtDesc(status, pageable).map(this::toResponse);
    }

    public WorkflowTaskResponse getTask(Long taskId) {
        return toResponse(findTask(taskId));
    }

    @Transactional
    public WorkflowApprovalResponse processApproval(Long taskId, Long approverId, WorkflowApprovalRequest req) {
        WorkflowTask task = findTask(taskId);
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));

        if (task.getStatus() == WorkflowTask.TaskStatus.APPROVED
                || task.getStatus() == WorkflowTask.TaskStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Task is already finalized");
        }

        WorkflowApproval approval = new WorkflowApproval();
        approval.setTask(task);
        approval.setApprover(approver);
        approval.setAction(req.getAction());
        approval.setComment(req.getComment());
        approvalRepository.save(approval);

        switch (req.getAction().toUpperCase()) {
            case "APPROVE" -> task.setStatus(WorkflowTask.TaskStatus.APPROVED);
            case "REJECT" -> task.setStatus(WorkflowTask.TaskStatus.REJECTED);
            case "RETURN" -> task.setStatus(WorkflowTask.TaskStatus.RETURNED);
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "Unknown action: " + req.getAction());
        }
        taskRepository.save(task);
        log.info("Task {} action={} by user={}", task.getTaskNumber(), req.getAction(), approverId);

        return new WorkflowApprovalResponse(approval.getId(), task.getId(),
                approver.getUsername(), approval.getAction(), approval.getComment(), approval.getCreatedAt());
    }

    public List<WorkflowApprovalResponse> getApprovals(Long taskId) {
        findTask(taskId);
        return approvalRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(a -> new WorkflowApprovalResponse(a.getId(), taskId,
                        a.getApprover().getUsername(), a.getAction(), a.getComment(), a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public int escalateOverdueTasks(LocalDateTime threshold) {
        List<WorkflowTask> tasks = taskRepository.findTasksToEscalate(threshold);
        tasks.forEach(t -> {
            t.setEscalated(true);
            t.setStatus(WorkflowTask.TaskStatus.ESCALATED);
            taskRepository.save(t);
            if (t.getAssignedTo() != null) {
                notificationService.createNotification(t.getAssignedTo().getId(),
                        Notification.NotificationType.TASK_ESCALATED,
                        "Task Escalated: " + t.getTaskNumber(),
                        "Task " + t.getTaskNumber() + " has been escalated due to inactivity.", t.getId());
            }
        });
        log.info("Escalated {} tasks", tasks.size());
        return tasks.size();
    }

    private WorkflowTask findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private String generateTaskNumber(WorkflowTask.TaskType type) {
        String prefix = switch (type) {
            case ROUTE_DATA_CHANGE -> "RDC";
            case REMINDER_RULE_CONFIG -> "RRC";
            case ABNORMAL_DATA_REVIEW -> "ADR";
        };
        return prefix + "-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now())
                + "-" + String.format("%04d", TASK_COUNTER.incrementAndGet());
    }

    private WorkflowTaskResponse toResponse(WorkflowTask t) {
        return new WorkflowTaskResponse(
                t.getId(), t.getTaskNumber(), t.getType().name(), t.getTitle(), t.getDescription(),
                t.getStatus().name(),
                t.getAssignedTo() != null ? t.getAssignedTo().getUsername() : null,
                t.getAssignedTo() != null ? t.getAssignedTo().getId() : null,
                t.getDeadline(), t.isEscalated(), t.getPayload(),
                t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}
