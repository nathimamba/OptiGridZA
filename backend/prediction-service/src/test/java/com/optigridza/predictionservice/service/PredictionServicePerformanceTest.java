package com.optigridza.predictionservice.service;

import com.optigridza.predictionservice.client.EtlServiceClient;
import com.optigridza.predictionservice.client.SimulationServiceClient;
import com.optigridza.predictionservice.dto.*;
import com.optigridza.predictionservice.model.PredictionRecord;
import com.optigridza.predictionservice.repository.PredictionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PredictionServicePerformanceTest {

    @Mock private EtlServiceClient etlClient;
    @Mock private SimulationServiceClient simulationClient;
    @Mock private PredictionRepository predictionRepository;
    @Mock private OnnxModelService onnxModelService;

    @InjectMocks
    private PredictionService predictionService;

    @BeforeEach
    void setup() {
        WeatherDto weather = new WeatherDto();
        weather.setSolarForecastKwh(5.0);
        GridStatusDto grid = new GridStatusDto();
        grid.setLoadSheddingStage(0);
        grid.setOutageProbability(0.1);
        TariffDto tariff = new TariffDto();
        tariff.setRatePerKwh(1.5);
        tariff.setPeakRate(3.0);

        when(etlClient.getWeather()).thenReturn(weather);
        when(etlClient.getGridStatus()).thenReturn(grid);
        when(etlClient.getCurrentTariff()).thenReturn(tariff);
        when(simulationClient.getSoc(anyString()))
                .thenReturn(SocResponse.builder().soc(70.0).capacityKwh(10.0).build());
        when(onnxModelService.predict(any()))
                .thenReturn(new OnnxModelService.PredictionResult("CHARGE", 0.85));
        when(onnxModelService.isModelLoaded()).thenReturn(true);
        when(predictionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Single recommendation completes within 100ms")
    void singleRecommendationLatency() {
        // Warm up
        predictionService.recommend("warm-up");

        long start = System.nanoTime();
        RecommendationResponse response = predictionService.recommend("company-perf");
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertThat(response).isNotNull();
        assertThat(response.getAction()).isEqualTo("CHARGE");
        assertThat(elapsed).as("Single recommendation should complete within 100ms").isLessThan(100);
    }

    @Test
    @DisplayName("100 sequential recommendations complete within 2 seconds")
    void sequentialThroughput() {
        // Warm up
        predictionService.recommend("warm-up");

        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            RecommendationResponse response = predictionService.recommend("company-" + i);
            assertThat(response).isNotNull();
        }
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertThat(elapsed).as("100 sequential recommendations should complete within 2s").isLessThan(2000);
    }

    @Test
    @DisplayName("50 concurrent recommendations complete without errors")
    void concurrentRecommendations() throws Exception {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<RecommendationResponse>> futures = new ArrayList<>();

        long start = System.nanoTime();
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> predictionService.recommend("company-" + idx)));
        }

        List<RecommendationResponse> results = new ArrayList<>();
        for (Future<RecommendationResponse> f : futures) {
            results.add(f.get(5, TimeUnit.SECONDS));
        }
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        executor.shutdown();

        assertThat(results).hasSize(threadCount);
        assertThat(results).allMatch(r -> r.getAction() != null);
        assertThat(elapsed).as("50 concurrent recommendations should complete within 5s").isLessThan(5000);
    }

    @Test
    @DisplayName("Fallback path performs well under load")
    void fallbackPerformance() {
        when(onnxModelService.predict(any())).thenReturn(null);
        when(onnxModelService.isModelLoaded()).thenReturn(false);

        long start = System.nanoTime();
        for (int i = 0; i < 200; i++) {
            RecommendationResponse response = predictionService.recommend("company-" + i);
            assertThat(response.getAction()).isEqualTo("HOLD");
        }
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertThat(elapsed).as("200 fallback recommendations should complete within 1s").isLessThan(1000);
    }

    @Test
    @DisplayName("History retrieval performs well with large dataset")
    void historyPerformance() {
        List<PredictionRecord> largeHistory = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeHistory.add(PredictionRecord.builder()
                    .companyId("company-1")
                    .action("CHARGE")
                    .confidence(0.85)
                    .build());
        }
        when(predictionRepository.findAllByCompanyIdOrderByCreatedAtDesc("company-1"))
                .thenReturn(largeHistory);

        long start = System.nanoTime();
        List<PredictionRecord> result = predictionService.getHistory("company-1");
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertThat(result).hasSize(1000);
        assertThat(elapsed).as("History retrieval of 1000 records should complete within 100ms").isLessThan(100);
    }
}
