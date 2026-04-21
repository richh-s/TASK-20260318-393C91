package com.citybus.platform.repository;

import com.citybus.platform.entity.BusRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusRouteRepository extends JpaRepository<BusRoute, Long> {

    Optional<BusRoute> findByRouteNumber(String routeNumber);

    @Query("SELECT r FROM BusRoute r WHERE r.status = 'ACTIVE' AND " +
           "(LOWER(r.routeNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(r.name) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<BusRoute> searchRoutes(@Param("q") String q);

    List<BusRoute> findByStatus(String status);
}
