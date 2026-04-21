package com.citybus.platform.controller;

import com.citybus.platform.common.ApiResponse;
import com.citybus.platform.dto.request.WorkflowApprovalRequest;
import com.citybus.platform.dto.request.WorkflowTaskRequest;
import com.citybus.platform.entity.User;
import com.citybus.platform.entity.WorkflowTask;
import com.citybus.platform.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping("/tasks")
    public ResponseEntity<ApiResponse<?>> createTask(@AuthenticationPrincipal User user,
                                                      @Valid @RequestBody WorkflowTaskRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(workflowService.createTask(user.getId(), req)));
    }

    @GetMapping("/tasks/my")
    public ResponseEntity<ApiResponse<?>> myTasks(@AuthenticationPrincipal User user,
                                                   @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(workflowService.getMyTasks(user.getId(), pageable)));
    }

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<?>> tasksByStatus(
            @RequestParam(required = false) WorkflowTask.TaskStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(workflowService.getTasksByStatus(status, pageable)));
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<ApiResponse<?>> getTask(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(workflowService.getTask(id)));
    }

    @PostMapping("/tasks/{id}/approvals")
    public ResponseEntity<ApiResponse<?>> approve(@AuthenticationPrincipal User user,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody WorkflowApprovalRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(workflowService.processApproval(id, user.getId(), req)));
    }

    @GetMapping("/tasks/{id}/approvals")
    public ResponseEntity<ApiResponse<?>> getApprovals(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(workflowService.getApprovals(id)));
    }
}
