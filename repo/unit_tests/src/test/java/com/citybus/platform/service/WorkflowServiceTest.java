package com.citybus.platform.service;

import com.citybus.platform.dto.request.WorkflowApprovalRequest;
import com.citybus.platform.dto.request.WorkflowTaskRequest;
import com.citybus.platform.dto.response.WorkflowApprovalResponse;
import com.citybus.platform.dto.response.WorkflowTaskResponse;
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
class WorkflowServiceTest {

    @Mock WorkflowTaskRepository taskRepository;
    @Mock WorkflowApprovalRepository approvalRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;

    @InjectMocks WorkflowService workflowService;

    private User creator;
    private User assignee;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(1L);
        creator.setUsername("dispatcher1");
        creator.setRole(User.UserRole.DISPATCHER);

        assignee = new User();
        assignee.setId(2L);
        assignee.setUsername("dispatcher2");
        assignee.setRole(User.UserRole.DISPATCHER);
    }

    @Test
    void createTask_withAssignee_savesTaskAndSendsNotification() {
        WorkflowTaskRequest req = new WorkflowTaskRequest();
        req.setType(WorkflowTask.TaskType.ROUTE_DATA_CHANGE);
        req.setTitle("Change route 101");
        req.setAssignedToId(2L);
        req.setDeadline(LocalDateTime.now().plusDays(3));

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(taskRepository.save(any())).thenAnswer(inv -> {
            WorkflowTask t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        WorkflowTaskResponse resp = workflowService.createTask(1L, req);

        assertThat(resp.getId()).isEqualTo(10L);
        assertThat(resp.getStatus()).isEqualTo("PENDING");
        assertThat(resp.getTaskNumber()).startsWith("RDC-");
        verify(notificationService).createNotification(eq(2L),
                eq(Notification.NotificationType.TASK_ASSIGNED), anyString(), anyString(), eq(10L));
    }

    @Test
    void createTask_withoutAssignee_noNotification() {
        WorkflowTaskRequest req = new WorkflowTaskRequest();
        req.setType(WorkflowTask.TaskType.ABNORMAL_DATA_REVIEW);
        req.setTitle("Review data anomaly");

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(taskRepository.save(any())).thenAnswer(inv -> {
            WorkflowTask t = inv.getArgument(0);
            t.setId(11L);
            return t;
        });

        WorkflowTaskResponse resp = workflowService.createTask(1L, req);
        assertThat(resp.getAssignedToUsername()).isNull();
        verifyNoInteractions(notificationService);
    }

    @Test
    void createTask_taskNumberPrefixMatchesType() {
        WorkflowTaskRequest req = new WorkflowTaskRequest();
        req.setType(WorkflowTask.TaskType.REMINDER_RULE_CONFIG);
        req.setTitle("Config reminder");
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkflowTaskResponse resp = workflowService.createTask(1L, req);
        assertThat(resp.getTaskNumber()).startsWith("RRC-");
    }

    @Test
    void processApproval_approve_setsStatusApproved() {
        WorkflowTask task = buildTask(WorkflowTask.TaskStatus.PENDING);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(approvalRepository.save(any())).thenAnswer(inv -> {
            WorkflowApproval a = inv.getArgument(0);
            a.setId(100L);
            return a;
        });
        when(taskRepository.save(any())).thenReturn(task);

        WorkflowApprovalRequest req = new WorkflowApprovalRequest();
        req.setAction("APPROVE");
        req.setComment("Looks good");

        WorkflowApprovalResponse resp = workflowService.processApproval(10L, 1L, req);

        assertThat(task.getStatus()).isEqualTo(WorkflowTask.TaskStatus.APPROVED);
        assertThat(resp.getAction()).isEqualTo("APPROVE");
    }

    @Test
    void processApproval_reject_setsStatusRejected() {
        WorkflowTask task = buildTask(WorkflowTask.TaskStatus.PENDING);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(approvalRepository.save(any())).thenAnswer(inv -> {
            WorkflowApproval a = inv.getArgument(0);
            a.setId(101L);
            return a;
        });
        when(taskRepository.save(any())).thenReturn(task);

        WorkflowApprovalRequest req = new WorkflowApprovalRequest();
        req.setAction("REJECT");

        workflowService.processApproval(10L, 1L, req);
        assertThat(task.getStatus()).isEqualTo(WorkflowTask.TaskStatus.REJECTED);
    }

    @Test
    void processApproval_return_setsStatusReturned() {
        WorkflowTask task = buildTask(WorkflowTask.TaskStatus.PENDING);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(approvalRepository.save(any())).thenAnswer(inv -> {
            WorkflowApproval a = inv.getArgument(0);
            a.setId(102L);
            return a;
        });
        when(taskRepository.save(any())).thenReturn(task);

        WorkflowApprovalRequest req = new WorkflowApprovalRequest();
        req.setAction("RETURN");

        workflowService.processApproval(10L, 1L, req);
        assertThat(task.getStatus()).isEqualTo(WorkflowTask.TaskStatus.RETURNED);
    }

    @Test
    void processApproval_alreadyApproved_throwsBadRequest() {
        WorkflowTask task = buildTask(WorkflowTask.TaskStatus.APPROVED);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

        WorkflowApprovalRequest req = new WorkflowApprovalRequest();
        req.setAction("APPROVE");

        assertThatThrownBy(() -> workflowService.processApproval(10L, 1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("finalized");
    }

    @Test
    void processApproval_unknownAction_throwsBadRequest() {
        WorkflowTask task = buildTask(WorkflowTask.TaskStatus.PENDING);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(approvalRepository.save(any())).thenAnswer(inv -> {
            WorkflowApproval a = inv.getArgument(0);
            a.setId(103L);
            return a;
        });

        WorkflowApprovalRequest req = new WorkflowApprovalRequest();
        req.setAction("INVALID");

        assertThatThrownBy(() -> workflowService.processApproval(10L, 1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unknown action");
    }

    @Test
    void escalateOverdueTasks_marksEscalatedAndNotifies() {
        WorkflowTask task = buildTask(WorkflowTask.TaskStatus.PENDING);
        task.setAssignedTo(assignee);
        when(taskRepository.findTasksToEscalate(any())).thenReturn(List.of(task));
        when(taskRepository.save(any())).thenReturn(task);

        int count = workflowService.escalateOverdueTasks(LocalDateTime.now().minusHours(25));

        assertThat(count).isEqualTo(1);
        assertThat(task.isEscalated()).isTrue();
        assertThat(task.getStatus()).isEqualTo(WorkflowTask.TaskStatus.ESCALATED);
        verify(notificationService).createNotification(eq(2L),
                eq(Notification.NotificationType.TASK_ESCALATED), anyString(), anyString(), any());
    }

    @Test
    void escalateOverdueTasks_noAssignee_noNotification() {
        WorkflowTask task = buildTask(WorkflowTask.TaskStatus.PENDING);
        when(taskRepository.findTasksToEscalate(any())).thenReturn(List.of(task));
        when(taskRepository.save(any())).thenReturn(task);

        workflowService.escalateOverdueTasks(LocalDateTime.now().minusHours(25));

        verifyNoInteractions(notificationService);
    }

    private WorkflowTask buildTask(WorkflowTask.TaskStatus status) {
        WorkflowTask task = new WorkflowTask();
        task.setId(10L);
        task.setTaskNumber("RDC-20260421-1001");
        task.setType(WorkflowTask.TaskType.ROUTE_DATA_CHANGE);
        task.setTitle("Test task");
        task.setStatus(status);
        task.setCreatedBy(creator);
        task.setEscalated(false);
        return task;
    }
}
