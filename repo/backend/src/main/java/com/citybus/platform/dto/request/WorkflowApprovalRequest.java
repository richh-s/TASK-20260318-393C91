package com.citybus.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkflowApprovalRequest {
    @NotBlank
    private String action;
    private String comment;
}
