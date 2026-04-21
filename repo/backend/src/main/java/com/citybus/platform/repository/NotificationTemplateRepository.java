package com.citybus.platform.repository;

import com.citybus.platform.entity.Notification;
import com.citybus.platform.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findByType(Notification.NotificationType type);
    Optional<NotificationTemplate> findByName(String name);
}
