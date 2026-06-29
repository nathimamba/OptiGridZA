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
public class GridStatusDto {
    private int loadSheddingStage;
    private double outageProbability;
    private String areaName;
    private LocalDateTime nextOutageStart;
    private LocalDateTime nextOutageEnd;
    private LocalDateTime fetchedAt;
}
