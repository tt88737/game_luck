package com.gameluck.wallet.service;

import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Wallet turnover task service.
 */
public interface IWalletTurnoverTaskService {

    void createFromCredit(String tenantId, WalletCreditBo bo, WalletTransaction transaction,
                          BigDecimal amount, BigDecimal requiredTurnover, Date now);

    int applyValidTurnover(String tenantId, Long memberId, String currencyCode, BigDecimal validTurnoverAmount, Date now);

    int cancelPendingByPurchase(String tenantId, Long memberId, String purchaseOrderNo,
                                String reversalNo, Date now);
}
