package com.tangluck.auth;

import java.math.BigDecimal;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record UserDto(
            Long userId,
            String email,
            String countryCode,
            String stateCode,
            String riskLevel,
            String status
    ) {
    }

    public record WalletDto(
            BigDecimal gcBalance,
            BigDecimal scBalance,
            BigDecimal scFrozen
    ) {
    }

    public record RegisterResponse(
            UserDto user,
            WalletDto wallet,
            String token,
            String accountType
    ) {
    }

    public record GuestRequest(
            String deviceId,
            String countryCode,
            String stateCode,
            String utmSource
    ) {
    }

    public record BindEmailRequest(
            String email,
            String password,
            java.time.LocalDate birthDate,
            String countryCode,
            String stateCode,
            java.util.List<AcceptedDocument> acceptedDocuments
    ) {
    }
}
