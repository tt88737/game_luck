package com.gameluck.wallet.service;

import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletManualAdjustBo;

/**
 * Wallet manual adjustment operations.
 */
public interface IWalletManualAdjustService {

    WalletTransaction adjust(WalletManualAdjustBo bo);
}
