package com.citybus.platform.service;

import com.citybus.platform.dto.request.ReservationRequest;
import com.citybus.platform.dto.response.ReservationResponse;
import com.citybus.platform.entity.*;
import com.citybus.platform.exception.BusinessException;
import com.citybus.platform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock ReservationRepository reservationRepository;
    @Mock UserRepository userRepository;
    @Mock BusRouteRepository routeRepository;
    @Mock BusStopRepository stopRepository;
    @Mock NotificationService notificationService;

    @InjectMocks ReservationService reservationService;

    private User user;
    private BusRoute route;
    private BusStop stop;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setRole(User.UserRole.PASSENGER);

        route = new BusRoute();
        route.setId(10L);
        route.setRouteNumber("101");
        route.setName("City Loop");

        stop = new BusStop();
        stop.setId(20L);
        stop.setNameEn("Zhongshan Road");
        stop.setNameCn("中山路");
        stop.setRoute(route);
        stop.setSequenceNumber(1);
        stop.setPopularityScore(80);
    }

    @Test
    void create_validRequest_savesReservationAndNotifies() {
        ReservationRequest req = new ReservationRequest();
        req.setRouteId(10L);
        req.setStopId(20L);
        req.setScheduledTime(LocalDateTime.now().plusDays(1));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(routeRepository.findById(10L)).thenReturn(Optional.of(route));
        when(stopRepository.findById(20L)).thenReturn(Optional.of(stop));
        when(reservationRepository.save(any())).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        ReservationResponse resp = reservationService.create(1L, req);

        assertThat(resp.getId()).isEqualTo(100L);
        assertThat(resp.getRouteNumber()).isEqualTo("101");
        assertThat(resp.getStatus()).isEqualTo("CONFIRMED");
        verify(notificationService).createNotification(eq(1L),
                eq(Notification.NotificationType.RESERVATION_SUCCESS), anyString(), anyString(), eq(100L));
    }

    @Test
    void create_stopNotOnRoute_throwsBadRequest() {
        BusRoute otherRoute = new BusRoute();
        otherRoute.setId(99L);
        stop.setRoute(otherRoute);

        ReservationRequest req = new ReservationRequest();
        req.setRouteId(10L);
        req.setStopId(20L);
        req.setScheduledTime(LocalDateTime.now().plusDays(1));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(routeRepository.findById(10L)).thenReturn(Optional.of(route));
        when(stopRepository.findById(20L)).thenReturn(Optional.of(stop));

        assertThatThrownBy(() -> reservationService.create(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Stop does not belong");
    }

    @Test
    void create_stopWithNullRoute_throwsBadRequest() {
        stop.setRoute(null);

        ReservationRequest req = new ReservationRequest();
        req.setRouteId(10L);
        req.setStopId(20L);
        req.setScheduledTime(LocalDateTime.now().plusDays(1));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(routeRepository.findById(10L)).thenReturn(Optional.of(route));
        when(stopRepository.findById(20L)).thenReturn(Optional.of(stop));

        assertThatThrownBy(() -> reservationService.create(1L, req))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void create_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        ReservationRequest req = new ReservationRequest();
        req.setRouteId(10L);
        req.setStopId(20L);
        req.setScheduledTime(LocalDateTime.now().plusDays(1));
        assertThatThrownBy(() -> reservationService.create(99L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void cancel_confirmedReservation_setsStatusCancelled() {
        Reservation res = new Reservation();
        res.setId(50L);
        res.setUser(user);
        res.setRoute(route);
        res.setStop(stop);
        res.setStatus(Reservation.ReservationStatus.CONFIRMED);

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(res));
        when(reservationRepository.save(any())).thenReturn(res);

        ReservationResponse resp = reservationService.cancel(50L, 1L);
        assertThat(resp.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancel_wrongUser_throwsForbidden() {
        User other = new User();
        other.setId(99L);
        Reservation res = new Reservation();
        res.setId(50L);
        res.setUser(other);
        res.setRoute(route);
        res.setStop(stop);
        res.setStatus(Reservation.ReservationStatus.CONFIRMED);

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(res));
        assertThatThrownBy(() -> reservationService.cancel(50L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void cancel_alreadyCancelled_throwsBadRequest() {
        Reservation res = new Reservation();
        res.setId(50L);
        res.setUser(user);
        res.setRoute(route);
        res.setStop(stop);
        res.setStatus(Reservation.ReservationStatus.CANCELLED);

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(res));
        assertThatThrownBy(() -> reservationService.cancel(50L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("confirmed");
    }

    @Test
    void getMyReservations_returnsMappedPage() {
        Reservation res = new Reservation();
        res.setId(50L);
        res.setUser(user);
        res.setRoute(route);
        res.setStop(stop);
        res.setStatus(Reservation.ReservationStatus.CONFIRMED);
        res.setScheduledTime(LocalDateTime.now().plusDays(1));

        Page<Reservation> page = new PageImpl<>(List.of(res));
        when(reservationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<ReservationResponse> result = reservationService.getMyReservations(1L, PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRouteNumber()).isEqualTo("101");
    }
}
