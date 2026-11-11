package com.itri623.userservice.repository;

import com.itri623.userservice.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByEmailOrderByTimestampDesc(String email);

    List<AuditEvent> findAllByOrderByTimestampDesc();
}