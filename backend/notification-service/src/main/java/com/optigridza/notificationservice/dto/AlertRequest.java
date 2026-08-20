package com.optigridza.notificationservice.dto;

import lombok.Data;

@Data
public class AlertRequest {
    private String companyId;
    private String alertType;
    private String severity;
    private String targetRole;
    private String message;
}
