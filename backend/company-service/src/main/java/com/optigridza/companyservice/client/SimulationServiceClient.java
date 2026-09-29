package com.optigridza.companyservice.client;

import com.optigridza.companyservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "simulation-service", configuration = FeignClientConfig.class)
public interface SimulationServiceClient {

    @PostMapping("/api/v1/simulation/battery/init")
    void initBattery(@RequestBody Map<String, String> body);
}