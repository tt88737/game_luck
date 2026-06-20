package com.tangluck.admin;

import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class AdminAuditService {
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    public AdminAuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
        this.clock = Clock.systemUTC();
    }

    public AuditLog write(
            AdminOperatorContext operator,
            String action,
            String targetType,
            String targetId,
            String beforeJson,
            String afterJson,
            String reason
    ) {
        return auditLogRepository.save(new AuditLog(
                operator.operatorId(),
                operator.operatorRole(),
                action,
                targetType,
                targetId,
                beforeJson,
                afterJson,
                reason,
                operator.ip(),
                clock.instant()
        ));
    }
}
