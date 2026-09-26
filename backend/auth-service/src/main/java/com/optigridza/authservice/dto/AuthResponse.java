package com.optigridza.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String companyId;
    private String message;
}