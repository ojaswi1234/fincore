package com.fincore.service;

import com.fincore.model.AuditEvent;
import com.fincore.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordEvent(Long userId, String username, String action, String resourceType, String resourceId, String ipAddress, String details) {
        AuditEvent event = new AuditEvent(userId, username, action, resourceType, resourceId, ipAddress, details);
        auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getRecentAuditLogs() {
        return auditEventRepository.findTop50ByOrderByCreatedAtDesc();
    }
}
