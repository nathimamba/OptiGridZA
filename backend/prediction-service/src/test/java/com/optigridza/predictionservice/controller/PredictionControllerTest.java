package com.optigridza.predictionservice.controller;

import com.optigridza.predictionservice.dto.RecommendationResponse;
import com.optigridza.predictionservice.model.PredictionRecord;
import com.optigridza.predictionservice.security.JwtService;
import com.optigridza.predictionservice.service.PredictionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PredictionController.class)
class PredictionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PredictionService predictionService;

    @MockitoBean
    private JwtService jwtService;

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http.csrf(c -> c.disable())
                .authorizeHttpRequests(a -> a.anyRequest().authenticated());
            return http.build();
        }
    }

    private RecommendationResponse sampleResponse() {
        return RecommendationResponse.builder()
                .companyId("company-1")
                .action("CHARGE")
                .confidence(0.85)
                .estimatedSavingsRand(15.0)
                .operatingMode("HYBRID")
                .reasoning("Good time to charge")
                .solarForecastKwh(5.0)
                .currentTariffRate(1.5)
                .peakTariffRate(3.0)
                .currentSoc(70.0)
                .estimatedLoad(3.5)
                .loadSheddingStage(0)
                .outageProbability(0.1)
                .validUntilEpoch(System.currentTimeMillis() + 1800000)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/prediction/history/{companyId}")
    class HistoryEndpoint {

        @Test
        @WithMockUser(roles = "SYSTEM_ADMIN")
        @DisplayName("Admin can access prediction history")
        void adminCanAccessHistory() throws Exception {
            PredictionRecord record = PredictionRecord.builder()
                    .companyId("company-1")
                    .action("CHARGE")
                    .confidence(0.85)
                    .estimatedSavingsRand(15.0)
                    .operatingMode("HYBRID")
                    .reasoning("Test")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(predictionService.getHistory("company-1")).thenReturn(List.of(record));

            mockMvc.perform(get("/api/v1/prediction/history/company-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].action").value("CHARGE"))
                    .andExpect(jsonPath("$[0].companyId").value("company-1"));
        }

        @Test
        @WithMockUser(roles = "ENERGY_MANAGER")
        @DisplayName("Energy manager can access prediction history")
        void energyManagerCanAccessHistory() throws Exception {
            when(predictionService.getHistory(anyString())).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/prediction/history/company-1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "VIEWER")
        @DisplayName("Viewer cannot access prediction history")
        void viewerCannotAccessHistory() throws Exception {
            mockMvc.perform(get("/api/v1/prediction/history/company-1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/prediction/model-info")
    class ModelInfoEndpoint {

        @Test
        @WithMockUser(roles = "SYSTEM_ADMIN")
        @DisplayName("Admin can access model info")
        void adminCanAccessModelInfo() throws Exception {
            mockMvc.perform(get("/api/v1/prediction/model-info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.modelType").exists())
                    .andExpect(jsonPath("$.version").value("1.0.0"))
                    .andExpect(jsonPath("$.actions").isArray());
        }

        @Test
        @WithMockUser(roles = "ENERGY_MANAGER")
        @DisplayName("Non-admin cannot access model info")
        void nonAdminCannotAccessModelInfo() throws Exception {
            mockMvc.perform(get("/api/v1/prediction/model-info"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Authentication required")
    class AuthRequired {

        @Test
        @DisplayName("Unauthenticated request is rejected")
        void unauthenticatedIsRejected() throws Exception {
            mockMvc.perform(get("/api/v1/prediction/history/company-1"))
                    .andExpect(status().is4xxClientError());
        }
    }
}
