package com.gameluck.wallet.service.impl;

import com.gameluck.wallet.domain.WalletAccount;
import com.gameluck.wallet.domain.WalletRelease;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.domain.bo.WalletFreezeOperationBo;
import com.gameluck.wallet.domain.vo.WalletRuleVo;
import com.gameluck.wallet.enums.WalletReleaseMode;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.mapper.WalletAccountMapper;
import com.gameluck.wallet.mapper.WalletFreezeMapper;
import com.gameluck.wallet.mapper.WalletReleaseMapper;
import com.gameluck.wallet.mapper.WalletTransactionMapper;
import com.gameluck.wallet.service.IWalletRuleService;
import com.gameluck.common.core.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    @Test
    @Tag("local")
    void creditAllowsManualAdjustToOverrideRuleReleaseMode() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        WalletReleaseMapper releaseMapper = mock(WalletReleaseMapper.class);
        WalletFreezeMapper freezeMapper = mock(WalletFreezeMapper.class);
        IWalletRuleService ruleService = mock(IWalletRuleService.class);
        WalletCoreServiceImpl service = new WalletCoreServiceImpl(
            accountMapper, transactionMapper, releaseMapper, freezeMapper, ruleService);

        WalletRuleVo rule = new WalletRuleVo();
        rule.setReleaseMode(WalletReleaseMode.MANUAL_REVIEW.name());
        rule.setDefaultRequiredTurnover(BigDecimal.ZERO);
        when(ruleService.resolveCreditRule(eq("000000"), eq("SC"), eq("MANUAL_ADJUST"))).thenReturn(rule);
        WalletAccount account = new WalletAccount();
        account.setAvailableBalance(BigDecimal.ZERO);
        account.setFrozenBalance(BigDecimal.ZERO);
        when(transactionMapper.selectByIdempotencyKey(eq("000000"), eq("manual-adjust:MA1"))).thenReturn(null);
        when(accountMapper.selectByBizKeyForUpdate(eq("000000"), eq(1001L), eq("SC"))).thenReturn(account);

        WalletCreditBo bo = creditBo("MANUAL_ADJUST", WalletReleaseMode.IMMEDIATE.name());
        bo.setManualAdjustOverride(true);

        WalletTransaction transaction = service.credit(bo);

        assertEquals(WalletReleaseMode.IMMEDIATE.name(), transaction.getReleaseMode());
        verify(transactionMapper).insert(any(WalletTransaction.class));
        verify(releaseMapper).insert(any(WalletRelease.class));
    }

    @Test
    @Tag("local")
    void creditRejectsManualAdjustReleaseModeOverrideWithoutInternalFlag() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        WalletReleaseMapper releaseMapper = mock(WalletReleaseMapper.class);
        WalletFreezeMapper freezeMapper = mock(WalletFreezeMapper.class);
        IWalletRuleService ruleService = mock(IWalletRuleService.class);
        WalletCoreServiceImpl service = new WalletCoreServiceImpl(
            accountMapper, transactionMapper, releaseMapper, freezeMapper, ruleService);

        WalletRuleVo rule = new WalletRuleVo();
        rule.setReleaseMode(WalletReleaseMode.MANUAL_REVIEW.name());
        rule.setDefaultRequiredTurnover(BigDecimal.ZERO);
        when(ruleService.resolveCreditRule(eq("000000"), eq("SC"), eq("MANUAL_ADJUST"))).thenReturn(rule);

        assertThrows(ServiceException.class,
            () -> service.credit(creditBo("MANUAL_ADJUST", WalletReleaseMode.IMMEDIATE.name())));
        verify(transactionMapper, never()).insert(any(WalletTransaction.class));
        verify(releaseMapper, never()).insert(any(WalletRelease.class));
    }

    @Test
    @Tag("local")
    void creditRejectsNonManualAdjustReleaseModeOverride() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        WalletReleaseMapper releaseMapper = mock(WalletReleaseMapper.class);
        WalletFreezeMapper freezeMapper = mock(WalletFreezeMapper.class);
        IWalletRuleService ruleService = mock(IWalletRuleService.class);
        WalletCoreServiceImpl service = new WalletCoreServiceImpl(
            accountMapper, transactionMapper, releaseMapper, freezeMapper, ruleService);

        WalletRuleVo rule = new WalletRuleVo();
        rule.setReleaseMode(WalletReleaseMode.MANUAL_REVIEW.name());
        rule.setDefaultRequiredTurnover(BigDecimal.ZERO);
        when(ruleService.resolveCreditRule(eq("000000"), eq("SC"), eq("PROMOTION"))).thenReturn(rule);

        WalletCreditBo bo = creditBo("PROMOTION", WalletReleaseMode.IMMEDIATE.name());
        bo.setManualAdjustOverride(true);

        assertThrows(ServiceException.class, () -> service.credit(bo));
        verify(transactionMapper, never()).insert(any(WalletTransaction.class));
        verify(releaseMapper, never()).insert(any(WalletRelease.class));
    }

    private static WalletCreditBo creditBo(String sourceType, String releaseMode) {
        WalletCreditBo bo = new WalletCreditBo();
        bo.setMemberId(1001L);
        bo.setCurrencyCode("SC");
        bo.setAmount(new BigDecimal("10"));
        bo.setSourceType(sourceType);
        bo.setBusinessNo("MA1");
        bo.setIdempotencyKey("manual-adjust:MA1");
        bo.setReleaseMode(releaseMode);
        bo.setRequiredTurnover(BigDecimal.ZERO);
        return bo;
    }
}
