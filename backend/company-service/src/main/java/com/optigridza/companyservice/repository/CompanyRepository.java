package com.optigridza.companyservice.repository;

import com.optigridza.companyservice.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, String> {
    boolean existsByName(String name);

    List<Company> findAllByActiveTrue();

    Optional<Company> findByIdAndActiveTrue(String id);
}
