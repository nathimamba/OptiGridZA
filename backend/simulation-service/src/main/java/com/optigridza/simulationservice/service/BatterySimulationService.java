package com.optigridza.simulationservice.service;

import com.optigridza.simulationservice.client.NotificationServiceClient;
import com.optigridza.simulationservice.dto.AlertRequest;
import com.optigridza.simulationservice.model.VirtualBattery;
import com.optigridza.simulationservice.repository.VirtualBatteryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BatterySimulationService {
    private static final double CHARGE_EFFICIENCY = 0.95;
    private static final double DISCHARGE_EFFICIENCY = 0.90;

    private final VirtualBatteryRepository repository;
    private final NotificationServiceClient notificationServiceClient;

    public double calculateNewSoc(double currentSoc, String action, double kwh, double capacityKwh) {
        return switch (action) {
            case "CHARGE" -> Math.min(100.0, currentSoc + (CHARGE_EFFICIENCY * kwh / capacityKwh * 100));
            case "DISCHARGE" -> Math.max(0.0, currentSoc - (kwh / DISCHARGE_EFFICIENCY / capacityKwh * 100));
            default -> currentSoc; // HOLD or SOLAR_PRIORITY
        };
    }

    public VirtualBattery applySimulation(String companyId, String action, double kwh, String mode) {
        VirtualBattery battery = repository.findByCompanyId(companyId)
                .orElseThrow(() -> new RuntimeException("No battery found for company " + companyId));

        double newSoc = calculateNewSoc(battery.getCurrentSoc(), action, kwh, battery.getCapacityKwh());

        if (newSoc < 20.0) {
            AlertRequest alertRequest = new AlertRequest();
            alertRequest.setCompanyId(companyId);
            alertRequest.setAlertType("LOW_SOC");
            alertRequest.setSeverity("CRITICAL");
            alertRequest.setTargetRole("TECHNICIAN");
            alertRequest.setMessage("Battery SOC dropped below 20% (" + newSoc + "%)");

            notificationServiceClient.createAlert(alertRequest);
        }

        battery.setCurrentSoc(newSoc);
        battery.setMode(mode);
        battery.setCycleCount(battery.getCycleCount() + 1);
        battery.setLastUpdated(LocalDateTime.now());

        return repository.save(battery);
    }

    public String getHealthStatus(double soc) {
        if (soc < 20) return "CRITICAL";
        if (soc < 50) return "WARNING";
        return "GOOD";
    }
}
