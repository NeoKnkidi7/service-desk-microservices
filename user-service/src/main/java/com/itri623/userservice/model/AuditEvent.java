package com.itri623.userservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;  // LOGIN_SUCCESS, LOGIN_FAILED, ACCESS_DENIED

    @Column(nullable = false)
    private String email;

    private Long userId;

    @Column(nullable = false)
    private String outcome;    // SUCCESS, FAILURE, DENIED

    private String ipAddress;

    private String details;    // Additional context (e.g., "Invalid token", "Wrong password")

    @Column(nullable = false)
    private LocalDateTime timestamp;

    // Constructors
    public AuditEvent() {}

    public AuditEvent(String eventType, String email, Long userId, String outcome, String ipAddress, String details) {
        this.eventType = eventType;
        this.email = email;
        this.userId = userId;
        this.outcome = outcome;
        this.ipAddress = ipAddress;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}