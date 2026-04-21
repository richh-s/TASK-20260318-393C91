package com.citybus.platform.dto.request;

import lombok.Data;

@Data
public class NotificationPrefRequest {
    private Long routeId;
    private Long stopId;
    private Integer reminderMinutes;
    private boolean enabled = true;
    private boolean dndEnabled;
    private String dndStart;
    private String dndEnd;
}
