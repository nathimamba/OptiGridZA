package com.optigridza.simulationservice.repository;


import com.optigridza.simulationservice.model.VirtualBattery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VirtualBatteryRepository extends JpaRepository<VirtualBattery, String> {
    Optional<VirtualBattery> findByCompanyId(String companyId);
}
