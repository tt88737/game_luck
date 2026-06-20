package com.tangluck.admin;

import com.tangluck.common.api.BusinessException;
import com.tangluck.common.api.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record AdminOperatorContext(
        Long operatorId,
        String operatorRole,
        Set<String> permissions,
        String ip
) {
    public static AdminOperatorContext from(HttpServletRequest request) {
        var permissions = permissionsFrom(request.getHeader("X-Admin-Permissions"));
        return new AdminOperatorContext(
                longHeader(request, "X-Admin-Operator-Id", 1L),
                stringHeader(request, "X-Admin-Operator-Role", "ops_admin"),
                permissions,
                clientIp(request)
        );
    }

    public void require(String permission) {
        if (!permissions.contains("*") && !permissions.contains(permission)) {
            throw new BusinessException(
                    ErrorCode.ADMIN_PERMISSION_DENIED,
                    "Admin permission denied.",
                    Map.of("permission", permission)
            );
        }
    }

    private static Set<String> permissionsFrom(String value) {
        if (value == null || value.isBlank()) return Set.of("*");
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Long longHeader(HttpServletRequest request, String name, Long fallback) {
        var value = request.getHeader(name);
        if (value == null || value.isBlank()) return fallback;
        return Long.parseLong(value);
    }

    private static String stringHeader(HttpServletRequest request, String name, String fallback) {
        var value = request.getHeader(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String clientIp(HttpServletRequest request) {
        var forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
