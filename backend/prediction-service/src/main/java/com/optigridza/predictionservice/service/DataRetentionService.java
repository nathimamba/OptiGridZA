package com.optigridza.predictionservice.service;

import com.optigridza.predictionservice.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataRetentionService {

    private final PredictionRepository predictionRepository;

    @Value("${prediction.retention.days:90}")
    private int retentionDays;

    @Scheduled(cron = "0 0 2 * * *") // Run daily at 2 AM
    @Transactional
    public void cleanupOldPredictions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int deleted = predictionRepository.deleteByCreatedAtBefore(cutoff);
        log.info("Data retention cleanup: deleted {} prediction records older than {} days", deleted, retentionDays);
    }
}
