package com.optigridza.predictionservice.service;

import com.optigridza.predictionservice.client.EtlServiceClient;
import com.optigridza.predictionservice.client.SimulationServiceClient;
import com.optigridza.predictionservice.dto.*;
import com.optigridza.predictionservice.model.PredictionRecord;
import com.optigridza.predictionservice.repository.PredictionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PredictionServiceTest {

    @Mock private EtlServiceClient etlClient;
    @Mock private SimulationServiceClient simulationClient;
    @Mock private PredictionRepository predictionRepository;
    @Mock private OnnxModelService onnxModelService;

    @InjectMocks
    private PredictionService predictionService;

    private static final String COMPANY_ID = "test-company-123";

    private WeatherDto defaultWeather() {
        WeatherDto w = new WeatherDto();
        w.setSolarForecastKwh(5.0);
        return w;
    }

    private GridStatusDto defaultGrid() {
        GridStatusDto g = new GridStatusDto();
        g.setLoadSheddingStage(0);
        g.setOutageProbability(0.1);
        return g;
    }

    private TariffDto defaultTariff() {
        TariffDto t = new TariffDto();
        t.setRatePerKwh(1.5);
        t.setPeakRate(3.0);
        return t;
    }

    private SocResponse defaultSoc() {
        return SocResponse.builder().soc(70.0).capacityKwh(10.0).build();
    }

    @BeforeEach
    void setupCommonMocks() {
        when(etlClient.getWeather()).thenReturn(defaultWeather());
        when(etlClient.getGridStatus()).thenReturn(defaultGrid());
        when(etlClient.getCurrentTariff()).thenReturn(defaultTariff());
        when(simulationClient.getSoc(COMPANY_ID)).thenReturn(defaultSoc());
        when(predictionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("ONNX Model Predictions")
    class OnnxPredictions {

        @Test
        @DisplayName("Should return CHARGE recommendation when model predicts CHARGE")
        void shouldReturnChargeWhenModelPredictsCharge() {
            when(onnxModelService.predict(any())).thenReturn(
                    new OnnxModelService.PredictionResult("CHARGE", 0.85));
            when(onnxModelService.isModelLoaded()).thenReturn(true);

            RecommendationResponse response = predictionService.recommend(COMPANY_ID);

            assertThat(response.getAction()).isEqualTo("CHARGE");
            assertThat(response.getConfidence()).isEqualTo(0.85);
            assertThat(response.getCompanyId()).isEqualTo(COMPANY_ID);
            assertThat(response.getEstimatedSavingsRand()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should return DISCHARGE recommendation when model predicts DISCHARGE")
        void shouldReturnDischarge() {
            when(onnxModelService.predict(any())).thenReturn(
                    new OnnxModelService.PredictionResult("DISCHARGE", 0.92));
            when(onnxModelService.isModelLoaded()).thenReturn(true);

            RecommendationResponse response = predictionService.recommend(COMPANY_ID);

            assertThat(response.getAction()).isEqualTo("DISCHARGE");
            assertThat(response.getConfidence()).isEqualTo(0.92);
        }

        @Test
        @DisplayName("Should return SOLAR_PRIORITY recommendation")
        void shouldReturnSolarPriority() {
            when(onnxModelService.predict(any())).thenReturn(
                    new OnnxModelService.PredictionResult("SOLAR_PRIORITY", 0.78));
            when(onnxModelService.isModelLoaded()).thenReturn(true);

            RecommendationResponse response = predictionService.recommend(COMPANY_ID);

            assertThat(response.getAction()).isEqualTo("SOLAR_PRIORITY");
            assertThat(response.getOperatingMode()).isEqualTo("HYBRID");
        }
    }

    @Nested
    @DisplayName("NFR-03 Safe Fallback")
    class SafeFallback {

        @Test
        @DisplayName("Should fallback to HOLD when model returns null")
        void shouldFallbackToHoldWhenModelReturnsNull() {
            when(onnxModelService.predict(any())).thenReturn(null);
            when(onnxModelService.isModelLoaded()).thenReturn(false);

            RecommendationResponse response = predictionService.recommend(COMPANY_ID);

            assertThat(response.getAction()).isEqualTo("HOLD");
            assertThat(response.getConfidence()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("HOLD should have zero estimated savings")
        void holdShouldHaveZeroSavings() {
            when(onnxModelService.predict(any())).thenReturn(null);
            when(onnxModelService.isModelLoaded()).thenReturn(false);

            RecommendationResponse response = predictionService.recommend(COMPANY_ID);

            assertThat(response.getEstimatedSavingsRand()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Operating Mode Determination")
    class ModeDetermination {

        @Test
        @DisplayName("Should return OFF_GRID when load shedding stage >= 4")
        void offGridOnHighLoadShedding() {
            GridStatusDto grid = defaultGrid();
            grid.setLoadSheddingStage(4);
            when(etlClient.getGridStatus()).thenReturn(grid);
            when(onnxModelService.predict(any())).thenReturn(
                    new OnnxModelService.PredictionResult("CHARGE", 0.8));
            when(onnxModelService.isModelLoaded()).thenReturn(true);

            RecommendationResponse response = predictionService.recommend(COMPANY_ID);

            assertThat(response.getOperatingMode()).isEqualTo("OFF_GRID");
        }

        @Test
        @DisplayName("Should return HYBRID when solar > 3kWh and load shedding <= 2")
        void hybridOnGoodSolar() {
            when(onnxModelService.predict(any())).thenReturn(
                    new OnnxModelService.PredictionResult("HOLD", 0.6));
            when(onnxModelService.isModelLoaded()).thenReturn(true);

            RecommendationResponse response = predictionService.recommend(COMPANY_ID);

            assertThat(response.getOperatingMode()).isEqualTo("HYBRID");
        }

        @Test
        @DisplayName("Should return GRID when solar is low and no load shedding")
        void gridOnLowSolar() {
            WeatherDto weather = defaultWeather();
            weather.setSolarForecastKwh(1.0);
            when(etlClient.getWeather()).thenReturn(weather);
            when(onnxModelService.predict(any())).thenReturn(
                    new OnnxModelService.PredictionResult("HOLD", 0.5));
            when(onnxModelService.isModelLoaded()).thenReturn(true);

            RecommendationResponse response = predictionService.recommend(COMPANY_ID);

            assertThat(response.getOperatingMode()).isEqualTo("GRID");
        }
    }

    @Nested
    @DisplayName("Savings Estimation")
    class SavingsEstimation {

        @Test
        @DisplayName("CHARGE savings = (peakRate - currentRate) * capacityKwh")
        void chargeSavings() {
            when(onnxModelService.predict(any())).thenReturn(
                    new OnnxModelService.PredictionResult("CHARGE", 0.9));
            when(onnxModelService.isModelLoaded()).thenReturn(true);

            RecommendationResponse response = predictionService.recommend(COMPANY_ID);

            double expected = (3.0 - 1.5) * 10.0; // (peak - current) * capacity
            assertThat(response.getEstimatedSavingsRand()).isEqualTo(expected);
        }

        @Test
        @DisplayName("SOLAR_PRIORITY savings = solarForecast * peakRate")
        void solarSavings() {
            when(onnxModelService.predict(any())).thenReturn(
                    new OnnxModelService.PredictionResult("SOLAR_PRIORITY", 0.85));
            when(onnxModelService.isModelLoaded()).thenReturn(true);

            RecommendationResponse response = predictionService.recommend(COMPANY_ID);

            double expected = 5.0 * 3.0; // solar * peakRate
            assertThat(response.getEstimatedSavingsRand()).isEqualTo(expected);
        }

        @Test
        @DisplayName("DISCHARGE savings = (soc/100) * capacityKwh * ratePerKwh")
        void dischargeSavings() {
            when(onnxModelService.predict(any())).thenReturn(
                    new OnnxModelService.PredictionResult("DISCHARGE", 0.88));
            when(onnxModelService.isModelLoaded()).thenReturn(true);

            RecommendationResponse response = predictionService.recommend(COMPANY_ID);

            double expected = (70.0 / 100.0) * 10.0 * 1.5; // (soc/100) * cap * rate
            assertThat(response.getEstimatedSavingsRand()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Should save prediction record to repository")
        void shouldSavePredictionRecord() {
            when(onnxModelService.predict(any())).thenReturn(
                    new OnnxModelService.PredictionResult("CHARGE", 0.9));
            when(onnxModelService.isModelLoaded()).thenReturn(true);

            predictionService.recommend(COMPANY_ID);

            ArgumentCaptor<PredictionRecord> captor = ArgumentCaptor.forClass(PredictionRecord.class);
            verify(predictionRepository).save(captor.capture());

            PredictionRecord saved = captor.getValue();
            assertThat(saved.getCompanyId()).isEqualTo(COMPANY_ID);
            assertThat(saved.getAction()).isEqualTo("CHARGE");
            assertThat(saved.getConfidence()).isEqualTo(0.9);
            assertThat(saved.getSolarForecastKwh()).isEqualTo(5.0);
            assertThat(saved.getCurrentSoc()).isEqualTo(70.0);
        }
    }

    @Nested
    @DisplayName("History")
    class History {

        @Test
        @DisplayName("Should return prediction history for company")
        void shouldReturnHistory() {
            PredictionRecord record = PredictionRecord.builder()
                    .companyId(COMPANY_ID)
                    .action("CHARGE")
                    .build();
            when(predictionRepository.findAllByCompanyIdOrderByCreatedAtDesc(COMPANY_ID))
                    .thenReturn(List.of(record));

            List<PredictionRecord> history = predictionService.getHistory(COMPANY_ID);

            assertThat(history).hasSize(1);
            assertThat(history.get(0).getAction()).isEqualTo("CHARGE");
        }
    }

    @Nested
    @DisplayName("Response Fields")
    class ResponseFields {

        @Test
        @DisplayName("Response should include all required fields")
        void responseShouldIncludeAllFields() {
            when(onnxModelService.predict(any())).thenReturn(
                    new OnnxModelService.PredictionResult("CHARGE", 0.85));
            when(onnxModelService.isModelLoaded()).thenReturn(true);

            RecommendationResponse response = predictionService.recommend(COMPANY_ID);

            assertThat(response.getCompanyId()).isNotNull();
            assertThat(response.getAction()).isNotNull();
            assertThat(response.getOperatingMode()).isNotNull();
            assertThat(response.getReasoning()).isNotNull();
            assertThat(response.getSolarForecastKwh()).isGreaterThan(0);
            assertThat(response.getCurrentTariffRate()).isGreaterThan(0);
            assertThat(response.getPeakTariffRate()).isGreaterThan(0);
            assertThat(response.getCurrentSoc()).isGreaterThan(0);
            assertThat(response.getValidUntilEpoch()).isGreaterThan(System.currentTimeMillis());
        }
    }
}
