package com.gameluck.wallet.service.impl;

import com.gameluck.wallet.domain.WalletAccount;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletFreezeOperationBo;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.mapper.WalletAccountMapper;
import com.gameluck.wallet.mapper.WalletFreezeMapper;
import com.gameluck.wallet.mapper.WalletReleaseMapper;
import com.gameluck.wallet.mapper.WalletTransactionMapper;
import com.gameluck.wallet.service.IWalletRuleService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletCoreServiceImplTest {

    @Test
    @Tag("local")
    void freezeReturnsFailedTransactionWhenAvailableBalanceIsInsufficient() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        WalletReleaseMapper releaseMapper = mock(WalletReleaseMapper.class);
        WalletFreezeMapper freezeMapper = mock(WalletFreezeMapper.class);
        IWalletRuleService ruleService = mock(IWalletRuleService.class);
        WalletCoreServiceImpl service = new WalletCoreServiceImpl(
            accountMapper, transactionMapper, releaseMapper, freezeMapper, ruleService);

        WalletAccount account = new WalletAccount();
        account.setAvailableBalance(new BigDecimal("3.000000"));
        account.setFrozenBalance(BigDecimal.ZERO);
        when(transactionMapper.selectByIdempotencyKey(eq("000000"), eq("redemption:freeze:RD1"))).thenReturn(null);
        when(accountMapper.selectByBizKeyForUpdate(eq("000000"), eq(1001L), eq("RC"))).thenReturn(account);

        WalletFreezeOperationBo bo = new WalletFreezeOperationBo();
        bo.setMemberId(1001L);
        bo.setCurrencyCode("RC");
        bo.setAmount(new BigDecimal("5.000000"));
        bo.setSourceType("REDEMPTION");
        bo.setBusinessNo("RD1");
        bo.setIdempotencyKey("redemption:freeze:RD1");

        WalletTransaction transaction = service.freeze(bo);

        assertEquals(WalletTransactionStatus.FAILED.name(), transaction.getStatus());
        assertEquals("INSUFFICIENT_BALANCE", transaction.getFailCode());
        verify(transactionMapper).insert(any(WalletTransaction.class));
    }
}
