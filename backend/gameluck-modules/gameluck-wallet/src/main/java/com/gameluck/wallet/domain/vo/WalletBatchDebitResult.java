package com.gameluck.wallet.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of an atomic multi-currency wallet debit.
 */
@Data
public class WalletBatchDebitResult {
    private String status;
    private List<WalletBatchDebitLineResult> lines = new ArrayList<>();
}
