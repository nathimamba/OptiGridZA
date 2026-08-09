package com.optigridza.simulationservice.controller;

import com.optigridza.simulationservice.dto.BatteryHealthResponse;
import com.optigridza.simulationservice.dto.SimulationRunRequest;
import com.optigridza.simulationservice.dto.SocResponse;
import com.optigridza.simulationservice.model.VirtualBattery;
import com.optigridza.simulationservice.repository.VirtualBatteryRepository;
import com.optigridza.simulationservice.service.BatterySimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class SimulationController {
    private final VirtualBatteryRepository  virtualBatteryRepository;
    private final BatterySimulationService simulationService;

    @GetMapping("/battery/{companyId}/soc")
    public ResponseEntity<SocResponse> getSoc(@PathVariable String companyId) {
        VirtualBattery battery = virtualBatteryRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new RuntimeException("No battery found for company " + companyId));
        return ResponseEntity.ok(SocResponse.builder()
                .soc(battery.getCurrentSoc())
                .capacityKwh(battery.getCapacityKwh())
                .build());
    }

    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('ENERGY_MANAGER','SYSTEM_ADMIN')")
    public ResponseEntity<VirtualBattery> runSimulation(@RequestBody SimulationRunRequest request) {
        VirtualBattery updated = simulationService.applySimulation(
                request.getCompanyId(), request.getAction(), request.getKwh(), request.getMode()
        );
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/battery/health/{companyId}")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ENERGY_MANAGER','SYSTEM_ADMIN')")
    public ResponseEntity<BatteryHealthResponse> getHealth(@PathVariable String companyId) {
        VirtualBattery battery = virtualBatteryRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new RuntimeException("No battery found for company " + companyId));

        BatteryHealthResponse response = BatteryHealthResponse.builder()
                .soc(battery.getCurrentSoc())
                .cycleCount(battery.getCycleCount())
                .efficiencyPct(battery.getEfficiencyPct())
                .healthStatus(simulationService.getHealthStatus(battery.getCurrentSoc()))
                .build();

        return ResponseEntity.ok(response);
    }
}
