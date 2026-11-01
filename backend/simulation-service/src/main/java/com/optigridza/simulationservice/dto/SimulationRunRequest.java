package com.optigridza.simulationservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SimulationRunRequest {
    @NotBlank
    private String companyId;
    @NotBlank
    private String action;
    @DecimalMin(value = "0.0", message = "kwh must be zero or greater")
    private double kwh;
    @NotBlank
    private String mode;
}
