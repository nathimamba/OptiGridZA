package com.optigridza.notificationservice.service;

import com.optigridza.notificationservice.dto.TicketRequest;
import com.optigridza.notificationservice.model.Ticket;
import com.optigridza.notificationservice.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository repository;

    public Ticket createTicket(TicketRequest request) {
        Ticket ticket = Ticket.builder()
                .companyId(request.getCompanyId())
                .title(request.getTitle())
                .description(request.getDescription())
                .ticketType(request.getTicketType())
                .priority(request.getPriority())
                .status("OPEN")
                .batterySoc(request.getBatterySoc())
                .batteryHealth(request.getBatteryHealth())
                .createdAt(LocalDateTime.now())
                .build();

        Ticket saved = repository.save(ticket);
        log.info("Ticket created: {} for company {} [{}]", saved.getId(), saved.getCompanyId(), saved.getTicketType());
        return saved;
    }

    public List<Ticket> getTicketsForCompany(String companyId) {
        return repository.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    public List<Ticket> getAllTickets() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public List<Ticket> getOpenTickets() {
        return repository.findByStatusOrderByCreatedAtDesc("OPEN");
    }

    public Ticket updateTicketStatus(String ticketId, String status, String resolutionNotes) {
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        ticket.setStatus(status);
        ticket.setUpdatedAt(LocalDateTime.now());

        if ("RESOLVED".equals(status) || "CLOSED".equals(status)) {
            ticket.setResolvedAt(LocalDateTime.now());
            if (resolutionNotes != null) {
                ticket.setResolutionNotes(resolutionNotes);
            }
        }

        log.info("Ticket {} status updated to {}", ticketId, status);
        return repository.save(ticket);
    }

    public Ticket assignTicket(String ticketId, String technicianEmail) {
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        ticket.setAssignedTo(technicianEmail);
        ticket.setStatus("IN_PROGRESS");
        ticket.setUpdatedAt(LocalDateTime.now());

        log.info("Ticket {} assigned to {}", ticketId, technicianEmail);
        return repository.save(ticket);
    }

    public List<Ticket> getTicketsForTechnician(String email) {
        return repository.findByAssignedToOrderByCreatedAtDesc(email);
    }
}
