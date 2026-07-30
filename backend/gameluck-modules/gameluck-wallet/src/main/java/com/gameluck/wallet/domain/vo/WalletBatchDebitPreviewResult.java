package com.gameluck.wallet.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WalletBatchDebitPreviewResult {
    private boolean sufficient;
    private List<WalletBatchDebitPreviewLineResult> lines = new ArrayList<>();
}
