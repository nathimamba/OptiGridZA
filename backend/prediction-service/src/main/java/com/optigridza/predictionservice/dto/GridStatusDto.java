package com.optigridza.predictionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GridStatusDto {
    private int loadSheddingStage;
    private double outageProbability;
    private String areaName;
    private LocalDateTime nextOutageStart;
    private LocalDateTime nextOutageEnd;
    private LocalDateTime fetchedAt;
}
