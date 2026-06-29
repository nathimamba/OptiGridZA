package com.optigridza.etlservice.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "weather_data")
public class WeatherData {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String city;

    // temperature in Celsius
    @Column(name = "temperature_celsius")
    private double temperatureCelsius;

    // cloud coverage percentage (0-100)
    // higher cloud cover = less solar generation
    @Column(name = "cloud_coverage_pct")
    private int cloudCoveragePct;

    // wind speed in m/s
    @Column(name = "wind_speed")
    private double windSpeed;

    // weather condition e.g. "Clear", "Clouds", "Rain"
    @Column(name = "weather_condition")
    private String weatherCondition;

    // estimated solar generation potential (0.0 to 1.0)
    // calculated from cloud cover and time of day
    @Column(name = "solar_potential")
    private double solarPotential;

    // forecast for next 24 hours in kWh per m2
    @Column(name = "solar_forecast_kwh")
    private double solarForecastKwh;

    // sunrise and sunset times — used for solar window calculation
    @Column(name = "sunrise_time")
    private LocalDateTime sunriseTime;

    @Column(name = "sunset_time")
    private LocalDateTime sunsetTime;

    @CreationTimestamp
    @Column(name = "fetched_at", updatable = false)
    private LocalDateTime fetchedAt;
}
