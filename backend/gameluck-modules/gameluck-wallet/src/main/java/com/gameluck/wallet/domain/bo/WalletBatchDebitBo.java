package com.gameluck.wallet.domain.bo;

import lombok.Data;

import java.util.List;

/**
 * Atomic multi-currency wallet debit request.
 */
@Data
public class WalletBatchDebitBo {
    private String tenantId;
    private Long memberId;
    private String businessNo;
    private String sourceType;
    private String remark;
    private List<WalletBatchDebitLineBo> lines;
}
