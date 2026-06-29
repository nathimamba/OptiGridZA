package com.optigridza.predictionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {
    private String companyId;
    private String action;
    private double confidence;
    private double estimatedSavingsRand;
    private String operatingMode;
    private String reasoning;
    private double solarForecastKwh;
    private double outageProbability;
    private double currentTariffRate;
    private double peakTariffRate;
    private double currentSoc;
    private double estimatedLoad;
    private int loadSheddingStage;
    private long validUntilEpoch;
}
