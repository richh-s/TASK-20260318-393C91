package com.citybus.platform.repository;

import com.citybus.platform.entity.WorkflowApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowApprovalRepository extends JpaRepository<WorkflowApproval, Long> {
    List<WorkflowApproval> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}
