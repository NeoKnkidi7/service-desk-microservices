package com.itri623.userservice.service;

import com.itri623.userservice.model.AuditEvent;
import com.itri623.userservice.repository.AuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    @Autowired
    private AuditRepository auditRepository;

    public AuditEvent logEvent(String eventType, String email, Long userId, String outcome, String ipAddress, String details) {
        AuditEvent event = new AuditEvent(eventType, email, userId, outcome, ipAddress, details);
        return auditRepository.save(event);
    }

    public List<AuditEvent> getRecentEvents(int limit) {
        return auditRepository.findAllByOrderByTimestampDesc()
                .stream().limit(limit).toList();
    }

    public List<AuditEvent> getEventsByEmail(String email) {
        return auditRepository.findByEmailOrderByTimestampDesc(email);
    }
}