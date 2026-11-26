package com.hookvault.audit;

import com.hookvault.audit.AuditLogRepository;
import com.hookvault.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logAction(
            UserPrincipal principal,
            String action,
            String entityType,
            UUID entityId,
            Map<String, Object> oldValues,
            Map<String, Object> newValues,
            String ipAddress,
            String userAgent
    ) {
        com.hookvault.user.User user = principal != null ? com.hookvault.user.User.builder().id(principal.getId()).build() : null;
        
        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValues(oldValues)
                .newValues(newValues)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(log);
    }

    @Transactional
    public void logAction(
            UUID userId,
            String action,
            String entityType,
            UUID entityId,
            Map<String, Object> oldValues,
            Map<String, Object> newValues
    ) {
        com.hookvault.user.User user = userId != null ? com.hookvault.user.User.builder().id(userId).build() : null;

        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValues(oldValues)
                .newValues(newValues)
                .build();

        auditLogRepository.save(log);
    }
}