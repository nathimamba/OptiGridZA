package com.optigridza.simulationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatteryHealthResponse {
    private double soc;
    private int cycleCount;
    private double efficiencyPct;
    private String healthStatus; // Condition
}
