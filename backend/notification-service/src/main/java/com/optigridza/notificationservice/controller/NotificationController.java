package com.optigridza.notificationservice.controller;

import com.optigridza.notificationservice.dto.AlertRequest;
import com.optigridza.notificationservice.model.Alert;
import com.optigridza.notificationservice.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Objects;
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final AlertService alertService;


    @PostMapping("/alerts")
    public ResponseEntity<Alert> createAlert(@RequestBody AlertRequest request) {
        Alert alert = alertService.createAlert(request);
        return ResponseEntity.ok(alert);
    }

    @GetMapping("/alerts/{companyId}")
    public ResponseEntity<List<Alert>> getAlerts(@PathVariable String companyId, Authentication auth) {
        boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));
        if (!admin && !Objects.equals(companyId, auth.getCredentials())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Company mismatch");
        }
        return ResponseEntity.ok(alertService.getAlertsForCompany(companyId));
    }

    @PutMapping("/alerts/{alertId}/acknowledge")
    public ResponseEntity<Alert> acknowledgeAlert(@PathVariable String alertId) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(alertId));
    }
}
