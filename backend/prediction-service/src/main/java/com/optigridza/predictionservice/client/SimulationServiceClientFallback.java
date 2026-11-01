package com.optigridza.predictionservice.client;

import com.optigridza.predictionservice.dto.SocResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
public class SimulationServiceClientFallback implements FallbackFactory<SimulationServiceClient> {
    @Override
    public SimulationServiceClient create(Throwable cause) {
        return companyId -> {
            log.warn("Simulation service failed for company {}: {}", companyId, cause.toString());
            if (cause instanceof FeignException feign && feign.status() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Company battery not found: " + companyId);
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Simulation service unavailable", cause);
        };
    }
}
