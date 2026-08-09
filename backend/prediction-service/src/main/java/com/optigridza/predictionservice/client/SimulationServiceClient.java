package com.optigridza.predictionservice.client;

import com.optigridza.predictionservice.config.FeignClientConfig;
import com.optigridza.predictionservice.dto.SocResponse;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "simulation-service", configuration = FeignClientConfig.class)
public interface SimulationServiceClient {
    @GetMapping("/api/v1/simulation/battery/{companyId}/soc")
    SocResponse getSoc(@PathVariable("companyId") String companyId);
}
