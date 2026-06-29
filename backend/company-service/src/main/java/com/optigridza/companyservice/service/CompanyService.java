package com.optigridza.companyservice.service;

import com.optigridza.companyservice.dto.AssignUserRequest;
import com.optigridza.companyservice.dto.CompanyResponse;
import com.optigridza.companyservice.dto.CompanyUserResponse;
import com.optigridza.companyservice.dto.CreateCompanyRequest;
import com.optigridza.companyservice.model.Company;
import com.optigridza.companyservice.model.CompanyUser;
import com.optigridza.companyservice.repository.CompanyRepository;
import com.optigridza.companyservice.repository.CompanyUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;

    public CompanyResponse createCompany(CreateCompanyRequest request) {

        if (companyRepository.existsByName(request.getName())) {
            throw new RuntimeException(
                    "Company already exists: " + request.getName());
        }

        Company company = Company.builder()
                .name(request.getName())
                .address(request.getAddress())
                .industryType(request.getIndustryType())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .active(true)
                .build();

        Company saved = companyRepository.save(company);
        log.info("Company created: {} id: {}", saved.getName(), saved.getId());
        return toCompanyResponse(saved, "Company created successfully");
    }

    public List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAllByActiveTrue()
                .stream()
                .map(c -> toCompanyResponse(c, null))
                .toList();
    }

    public CompanyResponse getCompany(String companyId) {
        Company company = companyRepository
                .findByIdAndActiveTrue(companyId)
                .orElseThrow(() -> new RuntimeException(
                        "Company not found: " + companyId));
        return toCompanyResponse(company, null);
    }

    public CompanyResponse deactivateCompany(String companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException(
                        "Company not found: " + companyId));
        company.setActive(false);
        companyRepository.save(company);
        log.info("Company deactivated: {}", companyId);
        return toCompanyResponse(company, "Company deactivated");
    }

    public CompanyUserResponse assignUser(
            String companyId, AssignUserRequest request) {

        Company company = companyRepository
                .findByIdAndActiveTrue(companyId)
                .orElseThrow(() -> new RuntimeException(
                        "Company not found: " + companyId));

        if (companyUserRepository.existsByUserEmailAndCompanyId(
                request.getUserEmail(), companyId)) {
            throw new RuntimeException(
                    request.getUserEmail() + " already assigned to this company");
        }

        if ("SYSTEM_ADMIN".equals(request.getRole())) {
            throw new RuntimeException(
                    "SYSTEM_ADMIN cannot be assigned to a company");
        }

        CompanyUser cu = CompanyUser.builder()
                .userEmail(request.getUserEmail())
                .companyId(companyId)
                .role(request.getRole())
                .active(true)
                .build();

        CompanyUser saved = companyUserRepository.save(cu);
        log.info("User {} → company {} as {}",
                saved.getUserEmail(), companyId, saved.getRole());

        return toUserResponse(saved, company.getName(),
                "User assigned successfully");
    }

    public List<CompanyUserResponse> getCompanyUsers(String companyId) {
        Company company = companyRepository
                .findByIdAndActiveTrue(companyId)
                .orElseThrow(() -> new RuntimeException(
                        "Company not found: " + companyId));
        return companyUserRepository.findAllByCompanyId(companyId)
                .stream()
                .map(cu -> toUserResponse(cu, company.getName(), null))
                .toList();
    }

    public CompanyUserResponse removeUser(
            String companyId, String userEmail) {
        Company company = companyRepository
                .findByIdAndActiveTrue(companyId)
                .orElseThrow(() -> new RuntimeException(
                        "Company not found: " + companyId));
        CompanyUser cu = companyUserRepository
                .findByUserEmailAndCompanyId(userEmail, companyId)
                .orElseThrow(() -> new RuntimeException(
                        userEmail + " not found in this company"));
        cu.setActive(false);
        companyUserRepository.save(cu);
        return toUserResponse(cu, company.getName(), "User removed");
    }


    private CompanyResponse toCompanyResponse(Company c, String message) {
        return CompanyResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .address(c.getAddress())
                .industryType(c.getIndustryType())
                .contactEmail(c.getContactEmail())
                .contactPhone(c.getContactPhone())
                .active(c.isActive())
                .createdAt(c.getCreatedAt())
                .message(message)
                .build();
    }

    private CompanyUserResponse toUserResponse(
            CompanyUser cu, String companyName, String message) {
        return CompanyUserResponse.builder()
                .id(cu.getId())
                .userEmail(cu.getUserEmail())
                .companyId(cu.getCompanyId())
                .companyName(companyName)
                .role(cu.getRole())
                .active(cu.isActive())
                .assignedAt(cu.getAssignedAt())
                .message(message)
                .build();
    }
}
