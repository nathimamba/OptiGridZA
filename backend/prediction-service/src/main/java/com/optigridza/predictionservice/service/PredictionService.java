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

    private static final double DEFAULT_LOAD = 3.5;private static final double BATTERY_KWH  = 10.0;

    public RecommendationResponse recommend(String companyId) {

        WeatherDto weather = etlClient.getWeather();
        GridStatusDto grid    = etlClient.getGridStatus();
        TariffDto tariff  = etlClient.getCurrentTariff();

        // double soc  = simulationClient.getSoc(companyId).getSoc();
        SocResponse socData = simulationClient.getSoc(companyId);
        double soc = socData.getSoc();
        double capacityKwh = socData.getCapacityKwh();

        double load = DEFAULT_LOAD;

        String action     = determineAction(weather, grid, tariff, soc);
        double confidence = calculateConfidence(action, grid, tariff, weather);
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

        log.info("Recommendation for {}: {} confidence:{} mode:{}",
                companyId, action, confidence, mode);

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

    private String determineAction(WeatherDto w, GridStatusDto g,
                                   TariffDto t, double soc) {
        int    stage = g.getLoadSheddingStage();
        double solar = w.getSolarForecastKwh();
        double rate  = t.getRatePerKwh();
        double peak  = t.getPeakRate();

        if (stage >= 4 && soc > 20)        return "DISCHARGE";
        if (soc < 15)                       return "CHARGE";
        if (solar > 6.0 && stage == 0)      return "SOLAR_PRIORITY";
        if (rate < peak * 0.6 && soc < 80) return "CHARGE";
        if (stage >= 2 && soc > 40)         return "DISCHARGE";
        if (solar > 4.0)                    return "SOLAR_PRIORITY";
        return "HOLD";
    }

    private double calculateConfidence(String action, GridStatusDto g,
                                       TariffDto t, WeatherDto w) {
        return switch (action) {
            case "DISCHARGE"      ->
                    g.getLoadSheddingStage() >= 4 ? 0.95 : 0.75;
            case "CHARGE"         ->
                    t.getRatePerKwh() < t.getPeakRate() * 0.5 ? 0.90 : 0.70;
            case "SOLAR_PRIORITY" ->
                    w.getSolarForecastKwh() > 6.0 ? 0.88 : 0.72;
            default -> 0.60;
        };
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
