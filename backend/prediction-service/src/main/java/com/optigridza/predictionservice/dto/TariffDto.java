package com.optigridza.predictionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffDto {
    private String periodType;
    private double ratePerKwh;
    private double peakRate;
    private double offPeakRate;
    private int periodStartHour;
    private int periodEndHour;
    private String season;
}
