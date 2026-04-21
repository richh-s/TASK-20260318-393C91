package com.citybus.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
public class WorkflowTaskResponse {
    private Long id;
    private String taskNumber;
    private String type;
    private String title;
    private String description;
    private String status;
    private String assignedToUsername;
    private Long assignedToId;
    private LocalDateTime deadline;
    private boolean escalated;
    private Map<String, Object> payload;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
