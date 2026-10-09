package com.optigridza.notificationservice.controller;

import com.optigridza.notificationservice.dto.TicketRequest;
import com.optigridza.notificationservice.model.Ticket;
import com.optigridza.notificationservice.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<Ticket> createTicket(@RequestBody TicketRequest request) {
        return ResponseEntity.ok(ticketService.createTicket(request));
    }

    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ENERGY_MANAGER','SYSTEM_ADMIN')")
    public ResponseEntity<List<Ticket>> getTicketsForCompany(@PathVariable String companyId) {
        return ResponseEntity.ok(ticketService.getTicketsForCompany(companyId));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<Ticket>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @GetMapping("/open")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ENERGY_MANAGER','SYSTEM_ADMIN')")
    public ResponseEntity<List<Ticket>> getOpenTickets() {
        return ResponseEntity.ok(ticketService.getOpenTickets());
    }

    @GetMapping("/assigned/{email}")
    @PreAuthorize("hasAnyRole('TECHNICIAN','SYSTEM_ADMIN')")
    public ResponseEntity<List<Ticket>> getTicketsForTechnician(@PathVariable String email) {
        return ResponseEntity.ok(ticketService.getTicketsForTechnician(email));
    }

    @PutMapping("/{ticketId}/status")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ENERGY_MANAGER','SYSTEM_ADMIN')")
    public ResponseEntity<Ticket> updateStatus(@PathVariable String ticketId,
                                               @RequestBody Map<String, String> body) {
        String status = body.get("status");
        String notes = body.get("resolutionNotes");
        return ResponseEntity.ok(ticketService.updateTicketStatus(ticketId, status, notes));
    }

    @PutMapping("/{ticketId}/assign")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ENERGY_MANAGER','SYSTEM_ADMIN')")
    public ResponseEntity<Ticket> assignTicket(@PathVariable String ticketId,
                                               @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ticketService.assignTicket(ticketId, body.get("email")));
    }
}
