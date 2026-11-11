package com.itri623.ticketservice.service;

import com.itri623.ticketservice.model.Ticket;
import com.itri623.ticketservice.repository.TicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final WebClient.Builder webClientBuilder;

    public TicketService(
            TicketRepository ticketRepository,
            WebClient.Builder webClientBuilder) {
        this.ticketRepository = ticketRepository;
        this.webClientBuilder = webClientBuilder;
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public Optional<Ticket> getTicketById(Long id) {
        return ticketRepository.findById(id);
    }

    public List<Ticket> getTicketsByUserId(Long userId) {
        return ticketRepository.findByUserId(userId);
    }

    public List<Ticket> getTicketsByStatus(String status) {
        return ticketRepository.findByStatus(status);
    }

    public Ticket createTicket(Ticket ticket) {

        if (!checkUserExists(ticket.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User with ID " + ticket.getUserId() + " does not exist"
            );
        }

        return ticketRepository.save(ticket);
    }

    public Ticket updateTicket(Long id, Ticket updatedTicket) {

        Ticket existingTicket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket with ID " + id + " not found"
                ));

        if (!checkUserExists(updatedTicket.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User with ID " + updatedTicket.getUserId() + " does not exist"
            );
        }

        existingTicket.setTitle(updatedTicket.getTitle());
        existingTicket.setDescription(updatedTicket.getDescription());
        existingTicket.setStatus(updatedTicket.getStatus());
        existingTicket.setUserId(updatedTicket.getUserId());
        existingTicket.setAssetId(updatedTicket.getAssetId());

        return ticketRepository.save(existingTicket);
    }

    public void deleteTicket(Long id) {
        if (!ticketRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Ticket with ID " + id + " not found"
            );
        }

        ticketRepository.deleteById(id);
    }

    private boolean checkUserExists(Long userId) {

        try {
            Boolean exists = webClientBuilder.build()
                    .get()
                    .uri("http://user-service/api/users/{id}/exists", userId)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();

            return Boolean.TRUE.equals(exists);

        } catch (Exception e) {
            return false;
        }
    }
}