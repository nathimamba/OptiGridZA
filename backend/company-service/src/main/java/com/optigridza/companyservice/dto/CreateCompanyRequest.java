package com.optigridza.companyservice.dto;

import lombok.Data;

@Data
public class CreateCompanyRequest {
    private String name;
    private String address;
    private String industryType;
    private String contactEmail;
    private String contactPhone;
    private Double latitude;
    private Double longitude;
}