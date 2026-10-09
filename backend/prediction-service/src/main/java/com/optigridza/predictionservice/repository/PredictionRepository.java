package com.optigridza.predictionservice.repository;

import com.optigridza.predictionservice.model.PredictionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<PredictionRecord, String> {
    Optional<PredictionRecord> findTopByCompanyIdOrderByCreatedAtDesc(
            String companyId);

    List<PredictionRecord> findAllByCompanyIdOrderByCreatedAtDesc(
            String companyId);

    @Modifying
    @Query("DELETE FROM PredictionRecord p WHERE p.createdAt < :cutoff")
    int deleteByCreatedAtBefore(LocalDateTime cutoff);
}
