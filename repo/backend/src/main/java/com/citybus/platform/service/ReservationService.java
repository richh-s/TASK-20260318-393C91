package com.citybus.platform.service;

import com.citybus.platform.dto.request.ReservationRequest;
import com.citybus.platform.dto.response.ReservationResponse;
import com.citybus.platform.entity.*;
import com.citybus.platform.exception.BusinessException;
import com.citybus.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final BusRouteRepository routeRepository;
    private final BusStopRepository stopRepository;
    private final NotificationService notificationService;

    @Transactional
    public ReservationResponse create(Long userId, ReservationRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
        BusRoute route = routeRepository.findById(req.getRouteId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Route not found"));
        BusStop stop = stopRepository.findById(req.getStopId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Stop not found"));

        if (stop.getRoute() == null || !stop.getRoute().getId().equals(route.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Stop does not belong to this route");
        }

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setRoute(route);
        reservation.setStop(stop);
        reservation.setScheduledTime(req.getScheduledTime());
        reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        notificationService.createNotification(userId, Notification.NotificationType.RESERVATION_SUCCESS,
                "Reservation Confirmed",
                "Your reservation for " + route.getRouteNumber() + " at " + stop.getNameEn()
                        + " on " + req.getScheduledTime() + " is confirmed.",
                reservation.getId());

        log.info("Reservation created: id={} user={} route={}", reservation.getId(), userId, route.getRouteNumber());
        return toResponse(reservation);
    }

    public Page<ReservationResponse> getMyReservations(Long userId, Pageable pageable) {
        return reservationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ReservationResponse cancel(Long reservationId, Long userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Reservation not found"));
        if (!reservation.getUser().getId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Only confirmed reservations can be cancelled");
        }
        reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        log.info("Reservation cancelled: id={}", reservationId);
        return toResponse(reservation);
    }

    private ReservationResponse toResponse(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getRoute().getId(), r.getRoute().getRouteNumber(), r.getRoute().getName(),
                r.getStop().getId(), r.getStop().getNameEn(), r.getStop().getNameCn(),
                r.getScheduledTime(), r.getStatus().name(), r.getCreatedAt()
        );
    }
}
