package com.optigridza.etlservice.model;

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
@Table(name = "tariff_data")
public class TariffData {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Eskom time-of-use periods
    // PEAK, OFF_PEAK, STANDARD
    @Column(name = "period_type", nullable = false)
    private String periodType;

    // rate in South African rand per kWh
    @Column(name = "rate_per_kwh", nullable = false)
    private double ratePerKwh;

    // when this tariff period starts (hour of day 0-23)
    @Column(name = "period_start_hour")
    private int periodStartHour;

    // when this tariff period ends (hour of day 0-23)
    @Column(name = "period_end_hour")
    private int periodEndHour;

    // season — SUMMER or WINTER (Eskom rates differ)
    @Column(name = "season")
    private String season;

    @CreationTimestamp
    @Column(name = "fetched_at", updatable = false)
    private LocalDateTime fetchedAt;
}
