package com.tangluck.p1;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class P1Dtos {
    private P1Dtos() {
    }

    public record ProductPackageDto(
            String packageCode,
            String name,
            BigDecimal priceAmount,
            String priceCurrency,
            BigDecimal gcAmount,
            boolean sandboxOnly
    ) {
    }

    public record CreatePurchaseOrderRequest(@NotBlank String packageCode) {
    }

    public record PurchaseOrderDto(
            String orderId,
            Long userId,
            String packageCode,
            BigDecimal priceAmount,
            String priceCurrency,
            String status,
            String provider,
            String currencyGranted,
            BigDecimal amountGranted,
            Instant createdAt
    ) {
    }

    public record KycApplicationRequest(
            @NotBlank String legalName,
            @NotNull LocalDate birthDate,
            @NotBlank String addressLine,
            @NotBlank String stateCode
    ) {
    }

    public record KycStatusDto(
            Long userId,
            String status,
            String legalName,
            String reviewReason,
            Instant updatedAt
    ) {
    }

    public record CreateRedemptionRequest(
            @NotNull @DecimalMin(value = "0.0001") BigDecimal scAmount,
            @NotBlank String method
    ) {
    }

    public record RedemptionDto(
            String redemptionId,
            Long userId,
            BigDecimal scAmount,
            String method,
            String status,
            boolean sandboxOnly,
            Instant createdAt
    ) {
    }

    public record P1OperationsDto(
            List<PurchaseOrderDto> purchaseOrders,
            List<KycStatusDto> kycApplications,
            List<RedemptionDto> redemptionRequests
    ) {
    }
}
