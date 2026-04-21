package com.citybus.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class WorkflowApprovalResponse {
    private Long id;
    private Long taskId;
    private String approverUsername;
    private String action;
    private String comment;
    private LocalDateTime createdAt;
}
