package com.gameluck.wallet.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletBatchDebitPreviewLineResult {
    private String currencyCode;
    private BigDecimal requiredAmount;
    private BigDecimal availableAmount;
    private BigDecimal shortfallAmount;
}
