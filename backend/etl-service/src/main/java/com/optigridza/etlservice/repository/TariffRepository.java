package com.optigridza.etlservice.repository;

import com.optigridza.etlservice.model.TariffData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


public interface TariffRepository extends JpaRepository<TariffData, String> {
    Optional<TariffData> findTopByPeriodTypeOrderByFetchedAtDesc(String periodType);
    List<TariffData> findAllBySeasonOrderByPeriodStartHour(String season);
}

