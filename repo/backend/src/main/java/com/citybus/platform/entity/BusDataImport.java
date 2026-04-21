package com.citybus.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bus_data_imports")
@Data
@NoArgsConstructor
public class BusDataImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_type", nullable = false, columnDefinition = "import_type")
    private ImportType importType;

    @Column(nullable = false)
    private int version = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "import_status")
    private ImportStatus status = ImportStatus.PENDING;

    @Column(name = "rows_parsed")
    private int rowsParsed = 0;

    @Column(name = "rows_failed")
    private int rowsFailed = 0;

    @Column(name = "error_message")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum ImportType { JSON, HTML }
    public enum ImportStatus { PENDING, PARSING, PARSED, FAILED }
}
