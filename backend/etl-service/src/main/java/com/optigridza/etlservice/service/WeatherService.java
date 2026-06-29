package com.optigridza.etlservice.service;

import com.optigridza.etlservice.dto.WeatherDto;
import com.optigridza.etlservice.model.WeatherData;
import com.optigridza.etlservice.repository.WeatherRepository;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {
    private final RestTemplate restTemplate;
    private final WeatherRepository weatherRepository;


    @Value("${api.openmeteo.base-url}")
    private String openMeteoBaseUrl;

    @Value("${api.location.city}")
    private String city;

    @Value("${api.location.lat}")
    private double lat;

    @Value("${api.location.lon}")
    private double lon;

    public WeatherData fetchAndSave() {
        try {
            // Open-Meteo — no API key needed
            // shortwave_radiation = solar irradiance in W/m²
            // directly measures energy hitting the panels
            String url = String.format(
                    "%s?latitude=%s&longitude=%s" +
                            "&current=temperature_2m,cloud_cover,wind_speed_10m,weather_code" +
                            "&daily=sunrise,sunset,shortwave_radiation_sum" +
                            "&timezone=Africa/Johannesburg",
                    openMeteoBaseUrl, lat, lon);

            Map response = restTemplate.getForObject(url, Map.class);
            if (response == null) return getLatestOrDefault();

            // parse current conditions
            Map current = (Map) response.get("current");
            double temperature   = toDouble(current.get("temperature_2m"));
            int    cloudCoverage = toInt(current.get("cloud_cover"));
            double windSpeed     = toDouble(current.get("wind_speed_10m"));
            int    weatherCode   = toInt(current.get("weather_code"));

            // parse daily solar data
            Map daily = (Map) response.get("daily");
            List radiationList = (List) daily.get("shortwave_radiation_sum");
            List sunriseList   = (List) daily.get("sunrise");
            List sunsetList    = (List) daily.get("sunset");

            // shortwave_radiation_sum is in MJ/m² per day
            // convert to kWh: divide by 3.6
            // then multiply by panel efficiency ~0.18 and area ~10m²
            double radiationMJ    = toDouble(radiationList.get(0));
            double solarForecastKwh = (radiationMJ / 3.6) * 0.18 * 10;

            // solar potential 0.0 to 1.0 from cloud cover
            double solarPotential = Math.max(0.1, 1.0 - (cloudCoverage / 100.0) * 0.9);

            // parse sunrise and sunset strings
            // Open-Meteo returns "2026-06-21T06:53"
            String sunriseStr = (String) sunriseList.get(0);
            String sunsetStr  = (String) sunsetList.get(0);
            LocalDateTime sunrise = LocalDateTime.parse(sunriseStr);
            LocalDateTime sunset  = LocalDateTime.parse(sunsetStr);

            // map WMO weather code to condition string
            String condition = mapWeatherCode(weatherCode);

            WeatherData weather = WeatherData.builder()
                    .city(city)
                    .temperatureCelsius(temperature)
                    .cloudCoveragePct(cloudCoverage)
                    .windSpeed(windSpeed)
                    .weatherCondition(condition)
                    .solarPotential(solarPotential)
                    .solarForecastKwh(solarForecastKwh)
                    .sunriseTime(sunrise)
                    .sunsetTime(sunset)
                    .build();

            WeatherData saved = weatherRepository.save(weather);
            log.info("Weather saved via Open-Meteo: {}°C clouds:{}% solar:{}kWh",
                    temperature, cloudCoverage, solarForecastKwh);
            return saved;

        } catch (Exception e) {
            log.error("Open-Meteo fetch failed: {}", e.getMessage());
            return getLatestOrDefault();
        }
    }

    // WMO weather interpretation codes
    // https://open-meteo.com/en/docs#weathervariables
    private String mapWeatherCode(int code) {
        if (code == 0)                  return "Clear";
        if (code <= 3)                  return "Partly Cloudy";
        if (code <= 48)                 return "Foggy";
        if (code <= 67)                 return "Rainy";
        if (code <= 77)                 return "Snowy";
        if (code <= 82)                 return "Showers";
        if (code <= 99)                 return "Thunderstorm";
        return "Unknown";
    }

    public WeatherDto getLatest() {
        return weatherRepository
                .findTopByCityOrderByFetchedAtDesc(city)
                .map(this::toDto)
                .orElseGet(this::defaultDto);
    }

    private WeatherData getLatestOrDefault() {
        return weatherRepository
                .findTopByCityOrderByFetchedAtDesc(city)
                .orElse(WeatherData.builder()
                        .city(city)
                        .solarForecastKwh(4.0)
                        .solarPotential(0.5)
                        .cloudCoveragePct(50)
                        .weatherCondition("Unknown")
                        .build());
    }

    private WeatherDto toDto(WeatherData w) {
        return WeatherDto.builder()
                .city(w.getCity())
                .temperatureCelsius(w.getTemperatureCelsius())
                .cloudCoveragePct(w.getCloudCoveragePct())
                .windSpeed(w.getWindSpeed())
                .weatherCondition(w.getWeatherCondition())
                .solarPotential(w.getSolarPotential())
                .solarForecastKwh(w.getSolarForecastKwh())
                .sunriseTime(w.getSunriseTime())
                .sunsetTime(w.getSunsetTime())
                .fetchedAt(w.getFetchedAt())
                .build();
    }

    private WeatherDto defaultDto() {
        return WeatherDto.builder()
                .city(city)
                .solarForecastKwh(4.0)
                .solarPotential(0.5)
                .weatherCondition("Unknown")
                .build();
    }

    private double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        return 0.0;
    }

    private int toInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        return 0;
    }

    private long toLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        return 0L;
    }
}
