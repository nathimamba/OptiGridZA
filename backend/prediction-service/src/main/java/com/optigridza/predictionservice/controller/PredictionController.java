package com.optigridza.predictionservice.controller;

import com.optigridza.predictionservice.dto.RecommendationResponse;
import com.optigridza.predictionservice.model.PredictionRecord;
import com.optigridza.predictionservice.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/prediction")
@RequiredArgsConstructor
public class PredictionController {
    private final PredictionService predictionService;

    @GetMapping("/recommend/{companyId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ENERGY_MANAGER','BUSINESS_OWNER')")
    public ResponseEntity<RecommendationResponse> recommend(
            @PathVariable String companyId,
            Authentication authentication) {

        String jwtCompanyId = (String) authentication.getCredentials();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority()
                        .equals("ROLE_SYSTEM_ADMIN"));

        if (!isAdmin && !companyId.equals(jwtCompanyId)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(
                predictionService.recommend(companyId));
    }

    @GetMapping("/history/{companyId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ENERGY_MANAGER')")
    public ResponseEntity<List<PredictionRecord>> getHistory(
            @PathVariable String companyId) {
        return ResponseEntity.ok(
                predictionService.getHistory(companyId));
    }

    @GetMapping("/model-info")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, Object>> modelInfo() {
        return ResponseEntity.ok(Map.of(
                "modelType", "Rule-based engine — ONNX model pending training",
                "features", List.of(
                        "solarForecastKwh", "outageProbability",
                        "currentTariffRate", "peakTariffRate",
                        "currentSoc", "estimatedLoad",
                        "loadSheddingStage"),
                "actions", List.of(
                        "CHARGE", "DISCHARGE", "HOLD", "SOLAR_PRIORITY"),
                "version", "1.0.0"
        ));
    }
}
