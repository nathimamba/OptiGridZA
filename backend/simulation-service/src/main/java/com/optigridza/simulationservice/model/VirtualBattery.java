package com.optigridza.simulationservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name="virtual_batteries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VirtualBattery {
    @Id
    private String id;

    @Column(name = "company_id", unique = true, nullable = false)
    private String companyId;

    @Column(name = "capacity_kwh", nullable = false)
    private double capacityKwh;

    @Column(name = "current_soc", nullable = false)
    private double currentSoc;

    @Column(name = "cycle_count", nullable = false)
    private int cycleCount;

    @Column(name = "efficiency_pct", nullable = false)
    private double efficiencyPct;

    @Column(nullable = false)
    private String mode; // GRID | HYBRID | OFF_GRID

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

}
