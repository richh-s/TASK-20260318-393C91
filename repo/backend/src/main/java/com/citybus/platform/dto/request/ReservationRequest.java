package com.citybus.platform.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationRequest {
    @NotNull
    private Long routeId;
    @NotNull
    private Long stopId;
    @NotNull
    private LocalDateTime scheduledTime;
}
