package com.optigridza.notificationservice.service;

import com.optigridza.notificationservice.dto.AlertRequest;
import com.optigridza.notificationservice.model.Alert;
import com.optigridza.notificationservice.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlertService {
    private final AlertRepository repository;

    public Alert createAlert(AlertRequest request) {
        Alert alert = Alert.builder()
                .id(UUID.randomUUID().toString())
                .companyId(request.getCompanyId())
                .alertType(request.getAlertType())
                .severity(request.getSeverity())
                .targetRole(request.getTargetRole())
                .message(request.getMessage())
                .createdAt(LocalDateTime.now())
                .acknowledged(false)
                .build();
        return repository.save(alert);
    }

    public List<Alert> getAlertsForCompany(String companyId) {
        return repository.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    public Alert acknowledgeAlert(String alertId) {
        Alert alert = repository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));
        alert.setAcknowledged(true);
        return repository.save(alert);
    }
}
