package com.gameluck.wallet.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Observed and recovered amounts for one batch debit currency.
 */
@Data
public class WalletBatchDebitLineResult {
    private String currencyCode;
    private BigDecimal requiredAmount;
    private BigDecimal availableAmount;
    private BigDecimal recoveredAmount;
    private BigDecimal shortfallAmount;
    private String walletTransactionNo;
}
