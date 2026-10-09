package com.optigridza.simulationservice.service;

import com.optigridza.simulationservice.client.NotificationServiceClient;
import com.optigridza.simulationservice.dto.AlertRequest;
import com.optigridza.simulationservice.dto.TicketRequest;
import com.optigridza.simulationservice.model.VirtualBattery;
import com.optigridza.simulationservice.repository.VirtualBatteryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatterySimulationService {
    private static final double CHARGE_EFFICIENCY = 0.95;
    private static final double DISCHARGE_EFFICIENCY = 0.90;
    private static final double DEGRADATION_THRESHOLD = 85.0;

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
        String healthStatus = getHealthStatus(newSoc);

        // Alert + Ticket for LOW SOC
        if (newSoc < 20.0) {
            AlertRequest alertRequest = new AlertRequest();
            alertRequest.setCompanyId(companyId);
            alertRequest.setAlertType("LOW_SOC");
            alertRequest.setSeverity("CRITICAL");
            alertRequest.setTargetRole("TECHNICIAN");
            alertRequest.setMessage("Battery SOC dropped below 20% (" + String.format("%.1f", newSoc) + "%)");
            notificationServiceClient.createAlert(alertRequest);

            createTicketForIssue(companyId, "LOW_SOC", "CRITICAL",
                    "Critical: Battery SOC at " + String.format("%.1f", newSoc) + "%",
                    "Battery state of charge has dropped to a critical level (" + String.format("%.1f", newSoc) + "%). "
                            + "Immediate attention required to prevent power loss. Action: " + action,
                    newSoc, healthStatus);
        }

        // Ticket for WARNING SOC (20-50%)
        if (newSoc >= 20.0 && newSoc < 50.0 && battery.getCurrentSoc() >= 50.0) {
            createTicketForIssue(companyId, "LOW_SOC", "HIGH",
                    "Warning: Battery SOC dropped below 50%",
                    "Battery state of charge has decreased to " + String.format("%.1f", newSoc) + "%. "
                            + "Monitor closely and consider charging to prevent further depletion.",
                    newSoc, healthStatus);
        }

        // Simulate efficiency degradation over cycles
        double newEfficiency = battery.getEfficiencyPct();
        if (battery.getCycleCount() > 0 && battery.getCycleCount() % 50 == 0) {
            newEfficiency = Math.max(60.0, newEfficiency - 0.5);
        }

        // Ticket for battery degradation
        if (newEfficiency < DEGRADATION_THRESHOLD && battery.getEfficiencyPct() >= DEGRADATION_THRESHOLD) {
            createTicketForIssue(companyId, "BATTERY_DEGRADATION", "HIGH",
                    "Battery efficiency dropped below " + DEGRADATION_THRESHOLD + "%",
                    "Battery efficiency has degraded to " + String.format("%.1f", newEfficiency) + "%. "
                            + "Cycle count: " + (battery.getCycleCount() + 1) + ". Consider maintenance or replacement.",
                    newSoc, healthStatus);
        }

        // Ticket for severe degradation
        if (newEfficiency < 70.0 && battery.getEfficiencyPct() >= 70.0) {
            AlertRequest alertRequest = new AlertRequest();
            alertRequest.setCompanyId(companyId);
            alertRequest.setAlertType("BATTERY_DEGRADATION");
            alertRequest.setSeverity("CRITICAL");
            alertRequest.setTargetRole("TECHNICIAN");
            alertRequest.setMessage("Battery efficiency critically low: " + String.format("%.1f", newEfficiency) + "%");
            notificationServiceClient.createAlert(alertRequest);

            createTicketForIssue(companyId, "BATTERY_DEGRADATION", "CRITICAL",
                    "Critical: Battery efficiency below 70%",
                    "Battery efficiency has dropped to " + String.format("%.1f", newEfficiency) + "%. "
                            + "Cycle count: " + (battery.getCycleCount() + 1) + ". Replacement recommended.",
                    newSoc, healthStatus);
        }

        battery.setCurrentSoc(newSoc);
        battery.setMode(mode);
        battery.setEfficiencyPct(newEfficiency);
        battery.setCycleCount(battery.getCycleCount() + 1);
        battery.setLastUpdated(LocalDateTime.now());

        return repository.save(battery);
    }

    private void createTicketForIssue(String companyId, String ticketType, String priority,
                                      String title, String description, double soc, String health) {
        try {
            TicketRequest ticket = TicketRequest.builder()
                    .companyId(companyId)
                    .title(title)
                    .description(description)
                    .ticketType(ticketType)
                    .priority(priority)
                    .batterySoc(soc)
                    .batteryHealth(health)
                    .build();
            notificationServiceClient.createTicket(ticket);
            log.info("Ticket created for company {}: {}", companyId, title);
        } catch (Exception e) {
            log.warn("Failed to create ticket for company {}: {}", companyId, e.getMessage());
        }
    }

    public String getHealthStatus(double soc) {
        if (soc < 20) return "CRITICAL";
        if (soc < 50) return "WARNING";
        return "GOOD";
    }
}
