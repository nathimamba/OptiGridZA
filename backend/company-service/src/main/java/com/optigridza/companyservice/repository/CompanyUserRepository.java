package com.optigridza.companyservice.repository;

import com.optigridza.companyservice.model.CompanyUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyUserRepository extends JpaRepository<CompanyUser, String> {
    List<CompanyUser> findAllByCompanyId(String companyId);

    boolean existsByUserEmailAndCompanyId(String userEmail, String companyId);

    Optional<CompanyUser> findByUserEmailAndCompanyId(
            String userEmail, String companyId);
}
