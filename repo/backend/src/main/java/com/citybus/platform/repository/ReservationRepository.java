package com.citybus.platform.repository;

import com.citybus.platform.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserIdOrderByScheduledTimeDesc(Long userId);

    org.springframework.data.domain.Page<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT r FROM Reservation r WHERE r.status = 'CONFIRMED' AND " +
           "r.scheduledTime BETWEEN :from AND :to")
    List<Reservation> findConfirmedBetween(@Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    @Query("SELECT r FROM Reservation r WHERE r.status = 'CONFIRMED' AND " +
           "r.scheduledTime < :threshold")
    List<Reservation> findOverdueReservations(@Param("threshold") LocalDateTime threshold);
}
