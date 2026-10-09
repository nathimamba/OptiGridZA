package com.optigridza.simulationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketRequest {
    private String companyId;
    private String title;
    private String description;
    private String ticketType;
    private String priority;
    private Double batterySoc;
    private String batteryHealth;
}
