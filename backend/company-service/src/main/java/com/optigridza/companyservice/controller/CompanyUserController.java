package com.optigridza.companyservice.controller;

import com.optigridza.companyservice.dto.AssignUserRequest;
import com.optigridza.companyservice.dto.CompanyUserResponse;
import com.optigridza.companyservice.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/users")
@RequiredArgsConstructor
public class CompanyUserController {
    private final CompanyService companyService;

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<CompanyUserResponse> assignUser(
            @PathVariable String companyId,
            @Valid @RequestBody AssignUserRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(companyService.assignUser(companyId, request));
    }

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<CompanyUserResponse>> getCompanyUsers(
            @PathVariable String companyId) {
        return ResponseEntity.ok(
                companyService.getCompanyUsers(companyId));
    }

    @DeleteMapping("/{userEmail}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<CompanyUserResponse> removeUser(
            @PathVariable String companyId,
            @PathVariable String userEmail) {
        return ResponseEntity.ok(
                companyService.removeUser(companyId, userEmail));
    }
}
