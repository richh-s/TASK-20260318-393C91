package com.citybus.platform.controller;

import com.citybus.platform.common.ApiResponse;
import com.citybus.platform.dto.request.ReservationRequest;
import com.citybus.platform.entity.User;
import com.citybus.platform.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ApiResponse<?>> create(@AuthenticationPrincipal User user,
                                                  @Valid @RequestBody ReservationRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(reservationService.create(user.getId(), req)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(@AuthenticationPrincipal User user,
                                                @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(reservationService.getMyReservations(user.getId(), pageable)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> cancel(@AuthenticationPrincipal User user,
                                                  @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(reservationService.cancel(id, user.getId())));
    }
}
