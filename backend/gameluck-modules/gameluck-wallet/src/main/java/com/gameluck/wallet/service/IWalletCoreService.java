package com.gameluck.wallet.service;

import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.domain.bo.WalletBatchDebitBo;
import com.gameluck.wallet.domain.bo.WalletDebitBo;
import com.gameluck.wallet.domain.bo.WalletFreezeOperationBo;
import com.gameluck.wallet.domain.bo.WalletTurnoverBo;
import com.gameluck.wallet.domain.vo.WalletBatchDebitResult;
import com.gameluck.wallet.domain.vo.WalletBatchDebitPreviewResult;

/**
 * Wallet core balance operations.
 */
public interface IWalletCoreService {

    WalletTransaction credit(WalletCreditBo bo);

    WalletTransaction debit(WalletDebitBo bo);

    WalletBatchDebitResult batchDebit(WalletBatchDebitBo bo);

    WalletBatchDebitPreviewResult previewBatchDebit(WalletBatchDebitBo bo);

    WalletTransaction freeze(WalletFreezeOperationBo bo);

    WalletTransaction unfreeze(WalletFreezeOperationBo bo);

    WalletTransaction settle(WalletFreezeOperationBo bo);

    int addValidTurnover(WalletTurnoverBo bo);
}
