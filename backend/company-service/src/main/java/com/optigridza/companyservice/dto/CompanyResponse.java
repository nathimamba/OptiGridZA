package com.optigridza.companyservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {
    private String id;
    private String name;
    private String address;
    private String industryType;
    private String contactEmail;
    private String contactPhone;
    private Double latitude;
    private Double longitude;
    private boolean active;
    private LocalDateTime createdAt;
    private String message;
}