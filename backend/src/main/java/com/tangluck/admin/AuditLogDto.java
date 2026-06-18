package com.tangluck.admin;

import java.time.Instant;

public record AuditLogDto(
        Long id,
        Long operatorId,
        String operatorRole,
        String action,
        String targetType,
        String targetId,
        String beforeJson,
        String afterJson,
        String ip,
        Instant createdAt
) {
}
