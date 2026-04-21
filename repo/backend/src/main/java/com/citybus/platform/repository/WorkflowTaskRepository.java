package com.citybus.platform.repository;

import com.citybus.platform.entity.WorkflowTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, Long> {

    Page<WorkflowTask> findByAssignedToIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<WorkflowTask> findByStatusOrderByCreatedAtDesc(WorkflowTask.TaskStatus status, Pageable pageable);

    Page<WorkflowTask> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<WorkflowTask> findByTaskNumber(String taskNumber);

    @Query("SELECT t FROM WorkflowTask t WHERE t.status = 'PENDING' AND " +
           "t.escalated = false AND t.createdAt < :threshold")
    List<WorkflowTask> findTasksToEscalate(@Param("threshold") LocalDateTime threshold);
}
