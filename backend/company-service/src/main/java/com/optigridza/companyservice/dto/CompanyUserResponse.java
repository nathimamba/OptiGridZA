package com.optigridza.companyservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompanyUserResponse {
    private String id;
    private String userEmail;
    private String companyId;
    private String companyName;
    private String role;
    private boolean active;
    private LocalDateTime assignedAt;
    private String message;
}
