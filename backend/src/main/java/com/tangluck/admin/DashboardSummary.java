package com.tangluck.admin;

import java.math.BigDecimal;

public record DashboardSummary(long registrations, long claims, BigDecimal scGranted, long riskEvents) {
}
