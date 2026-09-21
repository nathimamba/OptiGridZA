package com.optigridza.companyservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "company_users",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"email", "company_id"}
        ))
public class CompanyUser {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "company_id", nullable = false)
    private String companyId;

    // ENERGY_MANAGER, BUSINESS_OWNER, TECHNICIAN, VIEWER
    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;
}
