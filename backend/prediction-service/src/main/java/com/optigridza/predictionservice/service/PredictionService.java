package com.optigridza.predictionservice.service;

import com.optigridza.predictionservice.client.EtlServiceClient;
import com.optigridza.predictionservice.client.SimulationServiceClient;
import com.optigridza.predictionservice.dto.*;
import com.optigridza.predictionservice.model.PredictionRecord;
import com.optigridza.predictionservice.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionService {
    private final EtlServiceClient etlClient;
    private final PredictionRepository predictionRepository;
    private final SimulationServiceClient simulationClient;
    private final OnnxModelService onnxModelService;

    private static final double DEFAULT_LOAD = 3.5;
    private static final double BATTERY_KWH  = 10.0;

    public RecommendationResponse recommend(String companyId) {

        WeatherDto weather = etlClient.getWeather();
        GridStatusDto grid    = etlClient.getGridStatus();
        TariffDto tariff  = etlClient.getCurrentTariff();

        SocResponse socData = simulationClient.getSoc(companyId);
        double soc = socData.getSoc();
        double capacityKwh = socData.getCapacityKwh();

        double load = DEFAULT_LOAD;

        // Feature order must exactly match training: solarForecastKwh, outageProbability,
        // currentTariffRate, peakTariffRate, currentSoc, estimatedLoad, loadSheddingStage
        float[] features = new float[]{
                (float) weather.getSolarForecastKwh(),
                (float) grid.getOutageProbability(),
                (float) tariff.getRatePerKwh(),
                (float) tariff.getPeakRate(),
                (float) soc,
                (float) load,
                (float) grid.getLoadSheddingStage()
        };

        String action;
        double confidence;

        OnnxModelService.PredictionResult result = onnxModelService.predict(features);

        if (result != null) {
            action = result.action();
            confidence = result.confidence();
        } else {
            // NFR-03: safe fallback when model fails to load or inference fails
            action = "HOLD";
            confidence = 0.0;
            log.warn("Using NFR-03 safe HOLD fallback for company {}", companyId);
        }

        String mode       = determineMode(action, grid, weather);
        double savings    = estimateSavings(action, tariff, weather, soc, capacityKwh);
        String reasoning  = buildReasoning(action, grid, tariff, weather, soc);

        PredictionRecord record = PredictionRecord.builder()
                .companyId(companyId)
                .action(action)
                .confidence(confidence)
                .estimatedSavingsRand(savings)
                .operatingMode(mode)
                .reasoning(reasoning)
                .solarForecastKwh(weather.getSolarForecastKwh())
                .loadSheddingStage(grid.getLoadSheddingStage())
                .outageProbability(grid.getOutageProbability())
                .currentTariffRate(tariff.getRatePerKwh())
                .peakTariffRate(tariff.getPeakRate())
                .currentSoc(soc)
                .build();

        predictionRepository.save(record);

        log.info("Recommendation for {}: {} confidence:{} mode:{} (model loaded: {})",
                companyId, action, confidence, mode, onnxModelService.isModelLoaded());

        return RecommendationResponse.builder()
                .companyId(companyId)
                .action(action)
                .confidence(confidence)
                .estimatedSavingsRand(savings)
                .operatingMode(mode)
                .reasoning(reasoning)
                .solarForecastKwh(weather.getSolarForecastKwh())
                .outageProbability(grid.getOutageProbability())
                .currentTariffRate(tariff.getRatePerKwh())
                .peakTariffRate(tariff.getPeakRate())
                .currentSoc(soc)
                .estimatedLoad(load)
                .loadSheddingStage(grid.getLoadSheddingStage())
                .validUntilEpoch(System.currentTimeMillis() + 1800000)
                .build();
    }

    private String determineMode(String action,
                                 GridStatusDto g, WeatherDto w) {
        if (g.getLoadSheddingStage() >= 4)       return "OFF_GRID";
        if (w.getSolarForecastKwh() > 3.0
                && g.getLoadSheddingStage() <= 2) return "HYBRID";
        if ("SOLAR_PRIORITY".equals(action))      return "HYBRID";
        return "GRID";
    }

    private double estimateSavings(String action, TariffDto t,
                                   WeatherDto w, double soc, double capacityKwh) {
        return switch (action) {
            case "CHARGE" ->
                    (t.getPeakRate() - t.getRatePerKwh()) * capacityKwh;
            case "SOLAR_PRIORITY" ->
                    w.getSolarForecastKwh() * t.getPeakRate();
            case "DISCHARGE" ->
                    (soc / 100.0) * capacityKwh * t.getRatePerKwh();
            default -> 0.0;
        };
    }

    private String buildReasoning(String action, GridStatusDto g,
                                  TariffDto t, WeatherDto w, double soc) {
        return switch (action) {
            case "CHARGE" -> String.format(
                    "Current tariff (R%.2f/kWh) is %.0f%% below peak " +
                            "(R%.2f/kWh). Charging now saves money before peak window.",
                    t.getRatePerKwh(),
                    (1 - t.getRatePerKwh() / t.getPeakRate()) * 100,
                    t.getPeakRate());
            case "DISCHARGE" -> String.format(
                    "Load shedding stage %d detected. " +
                            "Battery at %.0f%%. Discharging to maintain operations.",
                    g.getLoadSheddingStage(), soc);
            case "SOLAR_PRIORITY" -> String.format(
                    "Strong solar forecast of %.1f kWh today. " +
                            "Maximising solar reduces grid draw and saves R%.2f.",
                    w.getSolarForecastKwh(),
                    w.getSolarForecastKwh() * t.getPeakRate());
            default ->
                    "Grid stable, tariff moderate, battery at " +
                            (int) soc + "%. No action required.";
        };
    }

    public List<PredictionRecord> getHistory(String companyId) {
        return predictionRepository
                .findAllByCompanyIdOrderByCreatedAtDesc(companyId);
    }
}