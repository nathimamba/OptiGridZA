package com.optigridza.companyservice.controller;

import com.optigridza.companyservice.dto.CompanyResponse;
import com.optigridza.companyservice.dto.CreateCompanyRequest;
import com.optigridza.companyservice.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService companyService;

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<CompanyResponse> createCompany(
            @Valid @RequestBody CreateCompanyRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(companyService.createCompany(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<CompanyResponse>> getAllCompanies() {
        return ResponseEntity.ok(companyService.getAllCompanies());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ENERGY_MANAGER','BUSINESS_OWNER','TECHNICIAN','VIEWER')")
    public ResponseEntity<CompanyResponse> getCompany(
            @PathVariable String id) {
        return ResponseEntity.ok(companyService.getCompany(id));
    }

    @GetMapping("/{id}/location")
    public ResponseEntity<Map<String, Double>> getCompanyLocation(
            @PathVariable String id) {
        return ResponseEntity.ok(companyService.getCompanyLocation(id));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<CompanyResponse> deactivateCompany(
            @PathVariable String id) {
        return ResponseEntity.ok(companyService.deactivateCompany(id));
    }
}