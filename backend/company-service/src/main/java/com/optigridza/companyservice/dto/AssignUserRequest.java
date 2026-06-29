package com.optigridza.companyservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignUserRequest {
    @Email(message = "Must be a valid email")
    @NotBlank(message = "User email is required")
    private String userEmail;

    @NotBlank(message = "Role is required")
    private String role;
}
