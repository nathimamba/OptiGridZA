package com.optigridza.simulationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private String id;
    private String companyId;
    private String alertType;
    private String severity;
    private String targetRole;
    private String message;
    private LocalDateTime createdAt;
    private boolean acknowledged;
}
