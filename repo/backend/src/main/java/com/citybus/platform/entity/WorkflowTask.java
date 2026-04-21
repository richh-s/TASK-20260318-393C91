package com.citybus.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "workflow_tasks")
@Data
@NoArgsConstructor
public class WorkflowTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_number", nullable = false, unique = true)
    private String taskNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "task_type")
    private TaskType type;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "task_status")
    private TaskStatus status = TaskStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    private LocalDateTime deadline;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "entity_type")
    private String entityType;

    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private Map<String, Object> payload;

    @Column(nullable = false)
    private boolean escalated = false;

    @OneToMany(mappedBy = "task", fetch = FetchType.LAZY)
    private List<WorkflowApproval> approvals;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum TaskType { ROUTE_DATA_CHANGE, REMINDER_RULE_CONFIG, ABNORMAL_DATA_REVIEW }
    public enum TaskStatus { PENDING, IN_PROGRESS, APPROVED, REJECTED, RETURNED, ESCALATED, CANCELLED }

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}
