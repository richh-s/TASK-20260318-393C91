package com.citybus.platform.dto.request;

import com.citybus.platform.entity.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TemplateRequest {
    @NotBlank
    private String name;
    @NotNull
    private Notification.NotificationType type;
    @NotBlank
    private String titleTemplate;
    @NotBlank
    private String contentTemplate;
    private Integer sensitivityLevel;
}
