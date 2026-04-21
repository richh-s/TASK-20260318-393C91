package com.citybus.platform.dto.request;

import com.citybus.platform.entity.WorkflowTask;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class WorkflowTaskRequest {
    @NotNull
    private WorkflowTask.TaskType type;
    @NotBlank
    private String title;
    private String description;
    private Long assignedToId;
    private LocalDateTime deadline;
    private Map<String, Object> payload;
}
