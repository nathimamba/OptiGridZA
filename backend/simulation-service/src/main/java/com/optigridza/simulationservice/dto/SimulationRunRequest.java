package com.optigridza.simulationservice.dto;

import lombok.Data;

@Data
public class SimulationRunRequest {
    private String companyId;
    private String action;
    private double kwh;
    private String mode;
}
