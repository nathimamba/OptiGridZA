package com.optigridza.predictionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
