package org.intelligentsia.monitoring.domain.monitoring.fraud.report.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_daily_reports")
@Getter @Setter @NoArgsConstructor
public class FraudDailyReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_date", nullable = false, unique = true)
    private LocalDate reportDate;

    @Column(name = "total_detected", nullable = false)
    private Integer totalDetected = 0;

    @Column(name = "total_processed", nullable = false)
    private Integer totalProcessed = 0;

    @Column(name = "total_unprocessed", nullable = false)
    private Integer totalUnprocessed = 0;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (generatedAt == null) generatedAt = LocalDateTime.now();
    }
}
