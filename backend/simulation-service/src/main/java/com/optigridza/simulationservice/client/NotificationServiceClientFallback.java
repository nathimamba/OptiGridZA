package com.optigridza.simulationservice.client;

import com.optigridza.simulationservice.dto.AlertRequest;
import com.optigridza.simulationservice.dto.AlertResponse;
import com.optigridza.simulationservice.dto.TicketRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
public class NotificationServiceClientFallback implements NotificationServiceClient {

    @Override
    public AlertResponse createAlert(AlertRequest request) {
        log.warn("Notification service unavailable — alert for company {} not delivered: {}",
                request.getCompanyId(), request.getMessage());
        return AlertResponse.builder()
                .id("fallback")
                .companyId(request.getCompanyId())
                .alertType(request.getAlertType())
                .severity(request.getSeverity())
                .message("[QUEUED] " + request.getMessage())
                .createdAt(LocalDateTime.now())
                .acknowledged(false)
                .build();
    }

    @Override
    public Object createTicket(TicketRequest request) {
        log.warn("Notification service unavailable — ticket for company {} not created: {}",
                request.getCompanyId(), request.getTitle());
        return Map.of("id", "fallback", "status", "QUEUED", "title", request.getTitle());
    }
}
