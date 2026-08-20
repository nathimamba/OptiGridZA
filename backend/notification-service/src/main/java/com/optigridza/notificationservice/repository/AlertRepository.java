package com.optigridza.notificationservice.repository;

import com.optigridza.notificationservice.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, String> {
    List<Alert> findByCompanyIdOrderByCreatedAtDesc(String companyId);
    List<Alert> findByCompanyIdAndTargetRoleOrderByCreatedAtDesc(String companyId, String targetRole);
}
