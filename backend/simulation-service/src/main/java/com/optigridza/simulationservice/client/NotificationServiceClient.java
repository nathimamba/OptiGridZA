package com.optigridza.simulationservice.client;

import com.optigridza.simulationservice.config.FeignClientConfig;
import com.optigridza.simulationservice.dto.AlertRequest;
import com.optigridza.simulationservice.dto.AlertResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", configuration = FeignClientConfig.class)
public interface NotificationServiceClient {
    @PostMapping("/api/v1/notifications/alerts")
    AlertResponse createAlert(@RequestBody AlertRequest request);
}
