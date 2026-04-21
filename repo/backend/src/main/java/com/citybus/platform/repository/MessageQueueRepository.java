package com.citybus.platform.repository;

import com.citybus.platform.entity.MessageQueueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageQueueRepository extends JpaRepository<MessageQueueItem, Long> {

    @Query("SELECT m FROM MessageQueueItem m WHERE m.status = 'PENDING' AND " +
           "(m.scheduledAt IS NULL OR m.scheduledAt <= :now) ORDER BY m.createdAt ASC")
    List<MessageQueueItem> findPendingMessages(@Param("now") LocalDateTime now);

    long countByStatus(MessageQueueItem.QueueStatus status);
}
