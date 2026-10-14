package org.intelligentsia.monitoring.domain.monitoring.atm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "atm_interface_monitoring_states")
@Getter
@Setter
@NoArgsConstructor
public class AtmInterfaceMonitoringState {
    @Id
    @Column(name = "int_code", length = 100)
    private String intCode;

    @Column(name = "interface_identifier")
    private String interfaceIdentifier;

    @Column(name = "interface_label")
    private String interfaceLabel;

    @Column(name = "current_status_code")
    private Integer currentStatusCode;

    @Column(name = "current_status_label")
    private String currentStatusLabel;

    @Column(name = "consecutive_status_code")
    private Integer consecutiveStatusCode;

    @Column(name = "consecutive_samples", nullable = false)
    private Integer consecutiveSamples = 0;

    @Column(name = "outage_started_at")
    private Instant outageStartedAt;

    @Column(name = "last_sample_at")
    private Instant lastSampleAt;

    @Column(name = "alert_raised_for_outage", nullable = false)
    private Boolean alertRaisedForOutage = false;

    @Column(name = "active_alert_id")
    private Long activeAlertId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    private void touch() {
        updatedAt = Instant.now();
    }
}
