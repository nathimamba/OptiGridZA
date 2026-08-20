package com.optigridza.notificationservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    @Id
    private String id;

    @Column(name = "company_id", nullable = false)
    private String companyId;

    @Column(name = "alert_type", nullable = false)
    private String alertType; // LOW_SOC | LOAD_SHEDDING_RISK | BATTERY_DEGRADATION

    @Column(nullable = false)
    private String severity; // INFO | WARNING | CRITICAL

    @Column(name = "target_role", nullable = false)
    private String targetRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean acknowledged;
}
