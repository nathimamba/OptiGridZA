package com.optigridza.predictionservice.model;

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
@Table(name="prediction_records")
public class PredictionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "company_id", nullable = false)
    private String companyId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private double confidence;

    @Column(name = "estimated_savings_rand")
    private double estimatedSavingsRand;

    @Column(name = "operating_mode")
    private String operatingMode;

    @Column(columnDefinition = "TEXT")
    private String reasoning;

    @Column(name = "solar_forecast_kwh")
    private double solarForecastKwh;

    @Column(name = "load_shedding_stage")
    private int loadSheddingStage;

    @Column(name = "outage_probability")
    private double outageProbability;

    @Column(name = "current_tariff_rate")
    private double currentTariffRate;

    @Column(name = "peak_tariff_rate")
    private double peakTariffRate;

    @Column(name = "current_soc")
    private double currentSoc;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
