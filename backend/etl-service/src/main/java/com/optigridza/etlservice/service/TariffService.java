package com.optigridza.etlservice.service;

import com.optigridza.etlservice.dto.TariffDto;
import com.optigridza.etlservice.model.TariffData;
import com.optigridza.etlservice.repository.TariffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TariffService {
    private final TariffRepository tariffRepository;

    // Eskom Megaflex tariff structure 2024/2025
    // Summer = September to May
    // Winter = June to August (higher rates)
    // Peak hours: 07:00-10:00 and 18:00-20:00 (weekdays)
    // Off-peak: 22:00-06:00
    // Standard: all other times

    public void initializeTariffs() {
        // only seed if table is empty
        if (tariffRepository.count() > 0) return;

        String season = getCurrentSeason();

        List<TariffData> tariffs = List.of(
                // Morning peak
                TariffData.builder()
                        .periodType("PEAK")
                        .ratePerKwh(season.equals("WINTER") ? 3.87 : 2.89)
                        .periodStartHour(7)
                        .periodEndHour(10)
                        .season(season)
                        .build(),
                // Evening peak
                TariffData.builder()
                        .periodType("PEAK")
                        .ratePerKwh(season.equals("WINTER") ? 3.87 : 2.89)
                        .periodStartHour(18)
                        .periodEndHour(20)
                        .season(season)
                        .build(),
                // Off-peak night
                TariffData.builder()
                        .periodType("OFF_PEAK")
                        .ratePerKwh(season.equals("WINTER") ? 1.12 : 0.98)
                        .periodStartHour(22)
                        .periodEndHour(6)
                        .season(season)
                        .build(),
                // Standard daytime
                TariffData.builder()
                        .periodType("STANDARD")
                        .ratePerKwh(season.equals("WINTER") ? 1.89 : 1.54)
                        .periodStartHour(10)
                        .periodEndHour(18)
                        .season(season)
                        .build()
        );

        tariffRepository.saveAll(tariffs);
        log.info("Tariff data initialized for season: {}", season);
    }

    public TariffDto getCurrentTariff() {
        int currentHour = LocalDateTime.now().getHour();
        log.info("DEBUG: current hour is {}", currentHour);
        // determine current period from hour
        String periodType = determinePeriod(currentHour);
        log.info("DEBUG: determined period is {}", periodType);
        TariffData tariff = tariffRepository
                .findTopByPeriodTypeOrderByFetchedAtDesc(periodType)
                .orElseGet(() -> defaultTariff(periodType));

        // also get peak rate for comparison in prediction
        double peakRate = tariffRepository
                .findTopByPeriodTypeOrderByFetchedAtDesc("PEAK")
                .map(TariffData::getRatePerKwh)
                .orElse(2.89);

        double offPeakRate = tariffRepository
                .findTopByPeriodTypeOrderByFetchedAtDesc("OFF_PEAK")
                .map(TariffData::getRatePerKwh)
                .orElse(0.98);

        return TariffDto.builder()
                .periodType(tariff.getPeriodType())
                .ratePerKwh(tariff.getRatePerKwh())
                .peakRate(peakRate)
                .offPeakRate(offPeakRate)
                .periodStartHour(tariff.getPeriodStartHour())
                .periodEndHour(tariff.getPeriodEndHour())
                .season(tariff.getSeason())
                .build();
    }

    private String determinePeriod(int hour) {
        if ((hour >= 7 && hour < 10) || (hour >= 18 && hour < 20)) {
            return "PEAK";
        } else if (hour >= 22 || hour < 6) {
            return "OFF_PEAK";
        }
        return "STANDARD";
    }

    private String getCurrentSeason() {
        int month = LocalDateTime.now().getMonthValue();
        // June, July, August = winter in South Africa
        return (month >= 6 && month <= 8) ? "WINTER" : "SUMMER";
    }

    private TariffData defaultTariff(String periodType) {
        return TariffData.builder()
                .periodType(periodType)
                .ratePerKwh(1.54)
                .periodStartHour(0)
                .periodEndHour(23)
                .season(getCurrentSeason())
                .build();
    }
}
