package com.citybus.platform.controller;

import com.citybus.platform.dto.request.WorkflowApprovalRequest;
import com.citybus.platform.dto.request.WorkflowTaskRequest;
import com.citybus.platform.dto.response.WorkflowApprovalResponse;
import com.citybus.platform.dto.response.WorkflowTaskResponse;
import com.citybus.platform.entity.WorkflowTask;
import com.citybus.platform.exception.GlobalExceptionHandler;
import com.citybus.platform.security.JwtTokenProvider;
import com.citybus.platform.service.UserDetailsServiceImpl;
import com.citybus.platform.service.WorkflowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WorkflowController.class, excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class})
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class WorkflowApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean WorkflowService workflowService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean UserDetailsServiceImpl userDetailsService;

    private WorkflowTaskResponse sampleTask() {
        return new WorkflowTaskResponse(
                1L, "RDC-20260421-1001", "ROUTE_DATA_CHANGE", "Title", "Desc",
                "PENDING", "dispatch2", 3L, LocalDateTime.now().plusDays(1),
                false, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void createTask_validRequest_returns200() throws Exception {
        when(workflowService.createTask(eq(2L), any(WorkflowTaskRequest.class))).thenReturn(sampleTask());

        WorkflowTaskRequest req = new WorkflowTaskRequest();
        req.setType(WorkflowTask.TaskType.ROUTE_DATA_CHANGE);
        req.setTitle("Title");

        mvc.perform(post("/api/workflow/tasks").with(csrf()).with(TestAuth.asDispatcher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskNumber").value("RDC-20260421-1001"));
    }

    @Test
    void createTask_blankTitle_returns400() throws Exception {
        WorkflowTaskRequest req = new WorkflowTaskRequest();
        req.setType(WorkflowTask.TaskType.ROUTE_DATA_CHANGE);
        req.setTitle("");

        mvc.perform(post("/api/workflow/tasks").with(csrf()).with(TestAuth.asDispatcher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTask_asPassenger_forbidden() throws Exception {
        WorkflowTaskRequest req = new WorkflowTaskRequest();
        req.setType(WorkflowTask.TaskType.ROUTE_DATA_CHANGE);
        req.setTitle("Title");

        mvc.perform(post("/api/workflow/tasks").with(csrf()).with(TestAuth.asPassenger())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void myTasks_returnsPagedResults() throws Exception {
        Page<WorkflowTaskResponse> page = new PageImpl<>(List.of(sampleTask()));
        when(workflowService.getMyTasks(eq(2L), any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/workflow/tasks/my").with(TestAuth.asDispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    @Test
    void tasksByStatus_returnsPagedResults() throws Exception {
        Page<WorkflowTaskResponse> page = new PageImpl<>(List.of(sampleTask()));
        when(workflowService.getTasksByStatus(eq(WorkflowTask.TaskStatus.PENDING), any(Pageable.class)))
                .thenReturn(page);

        mvc.perform(get("/api/workflow/tasks").param("status", "PENDING").with(TestAuth.asDispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].taskNumber").value("RDC-20260421-1001"));
    }

    @Test
    void getTask_returnsTask() throws Exception {
        when(workflowService.getTask(1L)).thenReturn(sampleTask());

        mvc.perform(get("/api/workflow/tasks/1").with(TestAuth.asDispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void approve_validRequest_returns200() throws Exception {
        WorkflowApprovalResponse resp = new WorkflowApprovalResponse(
                100L, 1L, "dispatch2", "APPROVE", "ok", LocalDateTime.now());
        when(workflowService.processApproval(eq(1L), eq(2L), any(WorkflowApprovalRequest.class)))
                .thenReturn(resp);

        WorkflowApprovalRequest req = new WorkflowApprovalRequest();
        req.setAction("APPROVE");
        req.setComment("ok");

        mvc.perform(post("/api/workflow/tasks/1/approvals")
                        .with(csrf()).with(TestAuth.asDispatcher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.action").value("APPROVE"));
    }

    @Test
    void approve_blankAction_returns400() throws Exception {
        WorkflowApprovalRequest req = new WorkflowApprovalRequest();
        req.setAction("");

        mvc.perform(post("/api/workflow/tasks/1/approvals")
                        .with(csrf()).with(TestAuth.asDispatcher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getApprovals_returnsList() throws Exception {
        WorkflowApprovalResponse r = new WorkflowApprovalResponse(
                1L, 1L, "dispatch2", "APPROVE", "ok", LocalDateTime.now());
        when(workflowService.getApprovals(1L)).thenReturn(List.of(r));

        mvc.perform(get("/api/workflow/tasks/1/approvals").with(TestAuth.asDispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].action").value("APPROVE"));
    }
}
