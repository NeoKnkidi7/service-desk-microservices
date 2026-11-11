package com.itri623.userservice.controller;

import com.itri623.userservice.model.AuditEvent;
import com.itri623.userservice.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @Autowired
    private AuditService auditService;

    @GetMapping
    public List<AuditEvent> getRecentAuditEvents() {
        return auditService.getRecentEvents(50);
    }

    @GetMapping("/email/{email}")
    public List<AuditEvent> getAuditEventsByEmail(@PathVariable String email) {
        return auditService.getEventsByEmail(email);
    }
}