package com.optigridza.etlservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WeatherDto {
    private String city;
    private double temperatureCelsius;
    private int cloudCoveragePct;
    private double windSpeed;
    private String weatherCondition;
    private double solarPotential;
    private double solarForecastKwh;
    private LocalDateTime sunriseTime;
    private LocalDateTime sunsetTime;
    private LocalDateTime fetchedAt;
}
