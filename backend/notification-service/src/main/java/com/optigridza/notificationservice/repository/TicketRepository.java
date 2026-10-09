package com.optigridza.notificationservice.repository;

import com.optigridza.notificationservice.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {
    List<Ticket> findByCompanyIdOrderByCreatedAtDesc(String companyId);
    List<Ticket> findByAssignedToOrderByCreatedAtDesc(String assignedTo);
    List<Ticket> findByStatusOrderByCreatedAtDesc(String status);
    List<Ticket> findByCompanyIdAndStatusOrderByCreatedAtDesc(String companyId, String status);
    List<Ticket> findAllByOrderByCreatedAtDesc();
}
