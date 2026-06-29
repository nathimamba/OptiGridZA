package com.optigridza.etlservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TariffDto {
    private String periodType;
    private double ratePerKwh;
    private double peakRate;
    private double offPeakRate;
    private int periodStartHour;
    private int periodEndHour;
    private String season;
}
