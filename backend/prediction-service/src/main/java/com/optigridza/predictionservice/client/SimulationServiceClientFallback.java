package com.optigridza.predictionservice.client;

import com.optigridza.predictionservice.dto.SocResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SimulationServiceClientFallback implements SimulationServiceClient {

    @Override
    public SocResponse getSoc(String companyId) {
        log.warn("Simulation service unavailable — returning safe SOC defaults for company {}", companyId);
        return SocResponse.builder()
                .soc(50.0)
                .capacityKwh(10.0)
                .build();
    }
}
