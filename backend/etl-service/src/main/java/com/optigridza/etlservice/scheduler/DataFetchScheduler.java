package com.optigridza.etlservice.scheduler;


import com.optigridza.etlservice.service.EskomService;
import com.optigridza.etlservice.service.TariffService;
import com.optigridza.etlservice.service.WeatherService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataFetchScheduler {
    private final WeatherService weatherService;
    private final EskomService eskomService;
    private final TariffService tariffService;

    // runs once at startup — seeds tariff data and does
    // the first weather and grid fetch immediately
    @PostConstruct
    public void init() {
        log.info("ETL service starting — initializing data...");
        tariffService.initializeTariffs();
        fetchAll();
    }

    // runs every hour — 3600000 milliseconds
    // fixedRate means it runs every hour from startup
    // regardless of how long the fetch takes
    @Scheduled(fixedRate = 3600000)
    public void fetchAll() {
        log.info("Scheduled data fetch starting...");

        try {
            weatherService.fetchAndSave();
            log.info("Weather fetch complete");
        } catch (Exception e) {
            log.error("Weather fetch failed: {}", e.getMessage());
        }

        try {
            eskomService.fetchAndSave();
            log.info("Eskom fetch complete");
        } catch (Exception e) {
            log.error("Eskom fetch failed: {}", e.getMessage());
        }

        log.info("Scheduled data fetch complete");
    }
}
