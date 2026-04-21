package com.citybus.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReservationResponse {
    private Long id;
    private Long routeId;
    private String routeNumber;
    private String routeName;
    private Long stopId;
    private String stopNameEn;
    private String stopNameCn;
    private LocalDateTime scheduledTime;
    private String status;
    private LocalDateTime createdAt;
}
