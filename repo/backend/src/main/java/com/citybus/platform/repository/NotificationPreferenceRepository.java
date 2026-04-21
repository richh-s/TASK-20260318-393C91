package com.citybus.platform.repository;

import com.citybus.platform.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    List<NotificationPreference> findByUserId(Long userId);

    Optional<NotificationPreference> findByUserIdAndRouteIdAndStopId(
            Long userId, Long routeId, Long stopId);

    List<NotificationPreference> findByUserIdAndEnabledTrue(Long userId);
}
