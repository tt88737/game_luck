package com.gameluck.wallet.domain.bo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Currency line in an atomic wallet debit request.
 */
@Data
public class WalletBatchDebitLineBo {
    private String currencyCode;
    private BigDecimal amount;
    private String idempotencyKey;
}
