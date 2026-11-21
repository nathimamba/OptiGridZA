package com.optigridza.simulationservice.controller;

import com.optigridza.simulationservice.dto.BatteryHealthResponse;
import com.optigridza.simulationservice.dto.SimulationRunRequest;
import jakarta.validation.Valid;
import com.optigridza.simulationservice.dto.SocResponse;
import com.optigridza.simulationservice.model.VirtualBattery;
import com.optigridza.simulationservice.repository.VirtualBatteryRepository;
import com.optigridza.simulationservice.service.BatterySimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final VirtualBatteryRepository virtualBatteryRepository;
    private final BatterySimulationService simulationService;

    private void requireTenant(String companyId, Authentication auth) {
        boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));
        if (!admin && !Objects.equals(companyId, auth.getCredentials())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Company mismatch");
        }
    }

    @GetMapping("/battery/{companyId}/soc")
    public ResponseEntity<SocResponse> getSoc(@PathVariable String companyId, Authentication auth) {
        requireTenant(companyId, auth);
        VirtualBattery battery = virtualBatteryRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No battery found for company " + companyId));
        return ResponseEntity.ok(SocResponse.builder()
                .soc(battery.getCurrentSoc())
                .capacityKwh(battery.getCapacityKwh())
                .build());
    }

    @PostMapping("/battery/init")
    @PreAuthorize("hasAnyRole('ENERGY_MANAGER','SYSTEM_ADMIN')")
    public ResponseEntity<VirtualBattery> initBattery(@RequestBody Map<String, String> body,
                                                      Authentication auth) {
        String companyId = body.get("companyId");
        requireTenant(companyId, auth);

        return virtualBatteryRepository.findByCompanyId(companyId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    VirtualBattery battery = VirtualBattery.builder()
                            .companyId(companyId)
                            .capacityKwh(10.0)
                            .currentSoc(100.0)
                            .cycleCount(0)
                            .efficiencyPct(100.0)
                            .mode("HYBRID")
                            .lastUpdated(LocalDateTime.now())
                            .build();
                    return ResponseEntity.ok(virtualBatteryRepository.save(battery));
                });
    }

    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('ENERGY_MANAGER','SYSTEM_ADMIN')")
    public ResponseEntity<VirtualBattery> runSimulation(@Valid @RequestBody SimulationRunRequest request,
                                                        Authentication auth) {
        requireTenant(request.getCompanyId(), auth);
        validateRun(request);
        VirtualBattery updated = simulationService.applySimulation(
                request.getCompanyId(), request.getAction(), request.getKwh(), request.getMode());
        return ResponseEntity.ok(updated);
    }

    private static final java.util.Set<String> ACTIONS =
            java.util.Set.of("CHARGE", "DISCHARGE", "HOLD", "SOLAR_PRIORITY");
    private static final java.util.Set<String> MODES =
            java.util.Set.of("GRID", "HYBRID", "OFF_GRID");

    
    private void validateRun(SimulationRunRequest r) {
        if (r.getCompanyId() == null || r.getCompanyId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId is required");
        }
        if (r.getAction() == null || !ACTIONS.contains(r.getAction())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "action must be one of " + ACTIONS);
        }
        if (r.getMode() == null || !MODES.contains(r.getMode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "mode must be one of " + MODES);
        }
        if (Double.isNaN(r.getKwh()) || r.getKwh() < 0 || r.getKwh() > 100000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "kwh must be between 0 and 100000");
        }
    }

    @GetMapping("/battery/health/{companyId}")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ENERGY_MANAGER','SYSTEM_ADMIN','BUSINESS_OWNER','VIEWER')")
    public ResponseEntity<BatteryHealthResponse> getHealth(@PathVariable String companyId,
                                                           Authentication auth) {
        requireTenant(companyId, auth);
        VirtualBattery battery = virtualBatteryRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new RuntimeException("No battery found for company " + companyId));

               return ResponseEntity.ok(BatteryHealthResponse.builder()
                .soc(battery.getCurrentSoc())
                .cycleCount(battery.getCycleCount())
                .efficiencyPct(battery.getEfficiencyPct())
                .healthStatus(simulationService.getHealthStatus(battery.getCurrentSoc()))
                .build());
    }
}
