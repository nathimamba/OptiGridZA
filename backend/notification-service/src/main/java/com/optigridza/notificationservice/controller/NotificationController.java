package com.optigridza.notificationservice.controller;

import com.optigridza.notificationservice.dto.AlertRequest;
import com.optigridza.notificationservice.model.Alert;
import com.optigridza.notificationservice.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final AlertService alertService;

    // Called by simulation-service when SOC < 20% (US-07)
    @PostMapping("/alerts")
    public ResponseEntity<Alert> createAlert(@RequestBody AlertRequest request) {
        Alert alert = alertService.createAlert(request);
        return ResponseEntity.ok(alert);
    }

    @GetMapping("/alerts/{companyId}")
    public ResponseEntity<List<Alert>> getAlerts(@PathVariable String companyId) {
        return ResponseEntity.ok(alertService.getAlertsForCompany(companyId));
    }

    @PutMapping("/alerts/{alertId}/acknowledge")
    public ResponseEntity<Alert> acknowledgeAlert(@PathVariable String alertId) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(alertId));
    }
}
