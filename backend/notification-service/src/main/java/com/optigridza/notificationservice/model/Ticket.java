package com.optigridza.notificationservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "company_id", nullable = false)
    private String companyId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "ticket_type", nullable = false)
    private String ticketType; // LOW_SOC | BATTERY_DEGRADATION | EFFICIENCY_DROP | MAINTENANCE

    @Column(nullable = false)
    private String priority; // LOW | MEDIUM | HIGH | CRITICAL

    @Column(nullable = false)
    private String status; // OPEN | IN_PROGRESS | RESOLVED | CLOSED

    @Column(name = "assigned_to")
    private String assignedTo; // technician email

    @Column(name = "battery_soc")
    private Double batterySoc;

    @Column(name = "battery_health")
    private String batteryHealth;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
