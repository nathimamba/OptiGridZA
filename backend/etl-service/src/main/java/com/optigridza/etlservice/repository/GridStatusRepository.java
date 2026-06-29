package com.optigridza.etlservice.repository;

import com.optigridza.etlservice.model.GridStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GridStatusRepository extends JpaRepository<GridStatus, String> {
    Optional<GridStatus> findTopByOrderByFetchedAtDesc();
}
