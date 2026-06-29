package com.optigridza.predictionservice.client;

import com.optigridza.predictionservice.dto.GridStatusDto;
import com.optigridza.predictionservice.dto.TariffDto;
import com.optigridza.predictionservice.dto.WeatherDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "etl-service")
public interface EtlServiceClient {
    @GetMapping("/api/v1/etl/weather")
    WeatherDto getWeather();

    @GetMapping("/api/v1/etl/grid-status")
    GridStatusDto getGridStatus();

    @GetMapping("/api/v1/etl/tariff/current")
    TariffDto getCurrentTariff();
}
