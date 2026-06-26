package com.gameluck.wallet.service;

import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.domain.bo.WalletDebitBo;
import com.gameluck.wallet.domain.bo.WalletTurnoverBo;

/**
 * Wallet core balance operations.
 */
public interface IWalletCoreService {

    WalletTransaction credit(WalletCreditBo bo);

    WalletTransaction debit(WalletDebitBo bo);

    int addValidTurnover(WalletTurnoverBo bo);
}
