package com.optigridza.etlservice.service;

import com.optigridza.etlservice.dto.GridStatusDto;
import com.optigridza.etlservice.model.GridStatus;
import com.optigridza.etlservice.repository.GridStatusRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EskomService {
    private final RestTemplate restTemplate;
    private final GridStatusRepository gridStatusRepository;

    @Value("${api.eskomsepush.key}")
    private String apiKey;

    @Value("${api.eskomsepush.base-url}")
    private String baseUrl;

    public GridStatus fetchAndSave() {
        try {
            // EskomSePush requires token in header
            HttpHeaders headers = new HttpHeaders();
            headers.set("token", apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // fetch current national load-shedding status
            String url = baseUrl + "/status";
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);

            Map body = response.getBody();
            if (body == null) {
                log.warn("EskomSePush returned null");
                return getLatestOrDefault();
            }

            // parse status
            Map status = (Map) body.get("status");
            Map capeTown = (Map) status.get("capetown");
            Map eskom    = (Map) status.get("eskom");

            // use eskom national stage as primary
            int stage = 0;
            if (eskom != null) {
                Object stageObj = eskom.get("stage");
                if (stageObj instanceof Number n) {
                    stage = n.intValue();
                }
            }

            // outage probability based on stage
            // stage 0 = 0%, stage 4 = 70%, stage 8 = 100%
            double probability = Math.min(1.0, stage * 0.125);

            GridStatus gridStatus = GridStatus.builder()
                    .loadSheddingStage(stage)
                    .outageProbability(probability)
                    .areaName("National (Eskom)")
                    .build();

            GridStatus saved = gridStatusRepository.save(gridStatus);
            log.info("Grid status saved: stage={} probability={}",
                    stage, probability);
            return saved;

        } catch (Exception e) {
            log.error("Failed to fetch Eskom status: {}", e.getMessage());
            return getLatestOrDefault();
        }
    }

    public GridStatusDto getLatest() {
        return gridStatusRepository
                .findTopByOrderByFetchedAtDesc()
                .map(this::toDto)
                .orElseGet(this::defaultDto);
    }

    private GridStatus getLatestOrDefault() {
        return gridStatusRepository
                .findTopByOrderByFetchedAtDesc()
                .orElse(GridStatus.builder()
                        .loadSheddingStage(0)
                        .outageProbability(0.0)
                        .areaName("Unknown")
                        .build());
    }

    private GridStatusDto toDto(GridStatus g) {
        return GridStatusDto.builder()
                .loadSheddingStage(g.getLoadSheddingStage())
                .outageProbability(g.getOutageProbability())
                .areaName(g.getAreaName())
                .nextOutageStart(g.getNextOutageStart())
                .nextOutageEnd(g.getNextOutageEnd())
                .fetchedAt(g.getFetchedAt())
                .build();
    }

    private GridStatusDto defaultDto() {
        return GridStatusDto.builder()
                .loadSheddingStage(0)
                .outageProbability(0.0)
                .areaName("Unknown")
                .build();
    }
}
