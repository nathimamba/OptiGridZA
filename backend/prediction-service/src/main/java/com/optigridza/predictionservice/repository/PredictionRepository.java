package com.optigridza.predictionservice.repository;

import com.optigridza.predictionservice.model.PredictionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<PredictionRecord, String> {
    Optional<PredictionRecord> findTopByCompanyIdOrderByCreatedAtDesc(
            String companyId);

    List<PredictionRecord> findAllByCompanyIdOrderByCreatedAtDesc(
            String companyId);
}
