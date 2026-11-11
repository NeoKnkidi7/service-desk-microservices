package com.itri623.ticketservice.controller;

import com.itri623.ticketservice.model.Ticket;
import com.itri623.ticketservice.service.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private static final Logger logger =
            LoggerFactory.getLogger(TicketController.class);

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public List<Ticket> getAllTickets() {
        return ticketService.getAllTickets();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable Long id) {
        return ticketService.getTicketById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public List<Ticket> getTicketsByUserId(@PathVariable Long userId) {
        return ticketService.getTicketsByUserId(userId);
    }

    @GetMapping("/status/{status}")
    public List<Ticket> getTicketsByStatus(@PathVariable String status) {
        return ticketService.getTicketsByStatus(status);
    }

    @PostMapping
    public ResponseEntity<Ticket> createTicket(@RequestBody Ticket ticket) {
        return ResponseEntity.ok(ticketService.createTicket(ticket));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTicket(
            @PathVariable Long id,
            @RequestBody Ticket ticket,
            HttpServletRequest request) {

        Optional<Ticket> existingOpt =
                ticketService.getTicketById(id);

        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (ticket.getStatus() != null
                && ticket.getStatus().equalsIgnoreCase("CLOSED")) {

            String role = request.getHeader("X-User-Role");

            if (role == null
                    || !"TECHNICIAN".equalsIgnoreCase(role)) {

                logger.warn(
                        "User with role {} attempted to close ticket {} but is not a TECHNICIAN",
                        role,
                        id
                );

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(
                                Map.of(
                                        "error",
                                        "Only TECHNICIANS can close tickets"
                                )
                        );
            }
        }

        Ticket updatedTicket =
                ticketService.updateTicket(id, ticket);

        return ResponseEntity.ok(updatedTicket);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}