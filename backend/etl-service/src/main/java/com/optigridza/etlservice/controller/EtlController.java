package com.optigridza.etlservice.controller;

import com.optigridza.etlservice.dto.GridStatusDto;
import com.optigridza.etlservice.dto.TariffDto;
import com.optigridza.etlservice.dto.WeatherDto;
import com.optigridza.etlservice.service.EskomService;
import com.optigridza.etlservice.service.TariffService;
import com.optigridza.etlservice.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/etl")
@RequiredArgsConstructor
public class EtlController {
    private final WeatherService weatherService;
    private final EskomService eskomService;
    private final TariffService tariffService;

    // GET /api/v1/etl/weather
    // returns latest weather and solar forecast
    @GetMapping("/weather")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WeatherDto> getWeather() {
        return ResponseEntity.ok(weatherService.getLatest());
    }

    // GET /api/v1/etl/grid-status
    // returns latest load-shedding stage and outage probability
    @GetMapping("/grid-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GridStatusDto> getGridStatus() {
        return ResponseEntity.ok(eskomService.getLatest());
    }

    // GET /api/v1/etl/tariff/current
    // returns current tariff period and rate
    @GetMapping("/tariff/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TariffDto> getCurrentTariff() {
        return ResponseEntity.ok(tariffService.getCurrentTariff());
    }

    // POST /api/v1/etl/fetch
    // manually trigger a data fetch — admin only
    @PostMapping("/fetch")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<String> triggerFetch() {
        weatherService.fetchAndSave();
        eskomService.fetchAndSave();
        return ResponseEntity.ok("Data fetch triggered successfully");
    }
}
