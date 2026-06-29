package com.optigridza.etlservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "grid_status")
public class GridStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // 0 = no load shedding, 8 = maximum
    @Column(name = "load_shedding_stage", nullable = false)
    private int loadSheddingStage;

    // probability of outage in the next hour (0.0 to 1.0)
    @Column(name = "outage_probability", nullable = false)
    private double outageProbability;

    // area name from EskomSePush
    @Column(name = "area_name")
    private String areaName;

    // next scheduled outage start time
    @Column(name = "next_outage_start")
    private LocalDateTime nextOutageStart;

    // next scheduled outage end time
    @Column(name = "next_outage_end")
    private LocalDateTime nextOutageEnd;

    @CreationTimestamp
    @Column(name = "fetched_at", updatable = false)
    private LocalDateTime fetchedAt;
}
