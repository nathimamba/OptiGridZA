package com.optigridza.companyservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCompanyRequest {
    @NotBlank(message = "Company name is required")
    private String name;

    private String address;
    private String industryType;

    @Email(message = "Must be a valid email")
    private String contactEmail;

    private String contactPhone;
}
