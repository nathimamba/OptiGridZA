package com.optigridza.authservice.dto;

import com.optigridza.authservice.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 12, max = 72, message = "Password must be 12 to 72 characters" )
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$",  message = "Password needs an upper-case letter, a lower-case letter, a digit and a symbol")
    private String password;

    private String companyId;

    private Role role;
}