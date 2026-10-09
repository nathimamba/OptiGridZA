package com.optigridza.predictionservice.client;

import com.optigridza.predictionservice.dto.GridStatusDto;
import com.optigridza.predictionservice.dto.TariffDto;
import com.optigridza.predictionservice.dto.WeatherDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EtlServiceClientFallback implements EtlServiceClient {

    @Override
    public WeatherDto getWeather() {
        log.warn("ETL service unavailable — returning safe weather defaults");
        WeatherDto dto = new WeatherDto();
        dto.setSolarForecastKwh(0.0);
        return dto;
    }

    @Override
    public GridStatusDto getGridStatus() {
        log.warn("ETL service unavailable — returning safe grid defaults (assume possible outage)");
        GridStatusDto dto = new GridStatusDto();
        dto.setLoadSheddingStage(0);
        dto.setOutageProbability(0.5);
        return dto;
    }

    @Override
    public TariffDto getCurrentTariff() {
        log.warn("ETL service unavailable — returning safe tariff defaults");
        TariffDto dto = new TariffDto();
        dto.setRatePerKwh(2.0);
        dto.setPeakRate(4.0);
        return dto;
    }
}
