package com.optigridza.etlservice.repository;

import com.optigridza.etlservice.model.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


public interface WeatherRepository extends JpaRepository<WeatherData, String> {
    Optional<WeatherData> findTopByCityOrderByFetchedAtDesc(String city);
}
