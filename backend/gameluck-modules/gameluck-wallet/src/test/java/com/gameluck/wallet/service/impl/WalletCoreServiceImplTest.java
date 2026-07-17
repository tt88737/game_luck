package com.gameluck.wallet.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.wallet.domain.WalletAccount;
import com.gameluck.wallet.domain.WalletFreeze;
import com.gameluck.wallet.domain.WalletRelease;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.domain.bo.WalletFreezeOperationBo;
import com.gameluck.wallet.enums.WalletReleaseMode;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.mapper.WalletAccountMapper;
import com.gameluck.wallet.mapper.WalletFreezeMapper;
import com.gameluck.wallet.mapper.WalletReleaseMapper;
import com.gameluck.wallet.mapper.WalletTransactionMapper;
import com.gameluck.wallet.service.IWalletTurnoverTaskService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        WalletCoreServiceImpl service = service(accountMapper, transactionMapper, releaseMapper, freezeMapper, existingMemberJdbcTemplate());

        WalletAccount account = new WalletAccount();
        account.setAvailableBalance(new BigDecimal("3.000000"));
        account.setFrozenBalance(BigDecimal.ZERO);
        when(transactionMapper.selectByIdempotencyKey(eq("000000"), eq("redemption:freeze:RD1"))).thenReturn(null);
        when(accountMapper.selectByBizKeyForUpdate(eq("000000"), eq(1001L), eq("RC"))).thenReturn(account);

        WalletFreezeOperationBo bo = freezeBo();

        WalletTransaction transaction = service.freeze(bo);

        assertEquals(WalletTransactionStatus.FAILED.name(), transaction.getStatus());
        assertEquals("INSUFFICIENT_BALANCE", transaction.getFailCode());
        verify(transactionMapper).insert(any(WalletTransaction.class));
    }

    @Test
    @Tag("local")
    void creditUsesExplicitReleaseModeFromBusinessRequest() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        WalletReleaseMapper releaseMapper = mock(WalletReleaseMapper.class);
        WalletFreezeMapper freezeMapper = mock(WalletFreezeMapper.class);
        WalletCoreServiceImpl service = service(accountMapper, transactionMapper, releaseMapper, freezeMapper, existingMemberJdbcTemplate());
        stubAccount(accountMapper, transactionMapper, "manual-adjust:MA1", "SC");

        WalletTransaction transaction = service.credit(creditBo("MANUAL_ADJUST", WalletReleaseMode.MANUAL_REVIEW.name()));

        assertEquals(WalletReleaseMode.MANUAL_REVIEW.name(), transaction.getReleaseMode());
        assertEquals(new BigDecimal("0.000000"), transaction.getRequiredTurnover());
        verify(transactionMapper).insert(any(WalletTransaction.class));
        verify(releaseMapper).insert(any(WalletRelease.class));
    }

    @Test
    @Tag("local")
    void creditDefaultsToImmediateReleaseWhenNoTurnoverRequired() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        WalletReleaseMapper releaseMapper = mock(WalletReleaseMapper.class);
        WalletFreezeMapper freezeMapper = mock(WalletFreezeMapper.class);
        WalletCoreServiceImpl service = service(accountMapper, transactionMapper, releaseMapper, freezeMapper, existingMemberJdbcTemplate());
        stubAccount(accountMapper, transactionMapper, "manual-adjust:MA1", "SC");

        WalletTransaction transaction = service.credit(creditBo("MANUAL_ADJUST", null));

        assertEquals(WalletReleaseMode.IMMEDIATE.name(), transaction.getReleaseMode());
        assertEquals(new BigDecimal("0.000000"), transaction.getRequiredTurnover());
        verify(transactionMapper).insert(any(WalletTransaction.class));
        verify(releaseMapper).insert(any(WalletRelease.class));
    }

    @Test
    @Tag("local")
    void creditDefaultsToAfterTurnoverWhenTurnoverRequired() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        WalletReleaseMapper releaseMapper = mock(WalletReleaseMapper.class);
        WalletFreezeMapper freezeMapper = mock(WalletFreezeMapper.class);
        WalletCoreServiceImpl service = service(accountMapper, transactionMapper, releaseMapper, freezeMapper, existingMemberJdbcTemplate());
        stubAccount(accountMapper, transactionMapper, "promotion:PR1", "SC");
        WalletCreditBo bo = creditBo("PROMOTION", null);
        bo.setBusinessNo("PR1");
        bo.setIdempotencyKey("promotion:PR1");
        bo.setTurnoverMultiplier(new BigDecimal("10"));

        WalletTransaction transaction = service.credit(bo);

        assertEquals(WalletReleaseMode.AFTER_TURNOVER.name(), transaction.getReleaseMode());
        assertEquals(new BigDecimal("100.000000"), transaction.getRequiredTurnover());
        verify(transactionMapper).insert(any(WalletTransaction.class));
        verify(releaseMapper).insert(any(WalletRelease.class));
    }

    @Test
    @Tag("local")
    void creditCreatesTurnoverTaskWhenRequiredAmountIsPositive() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        WalletReleaseMapper releaseMapper = mock(WalletReleaseMapper.class);
        WalletFreezeMapper freezeMapper = mock(WalletFreezeMapper.class);
        IWalletTurnoverTaskService turnoverTaskService = mock(IWalletTurnoverTaskService.class);
        WalletCoreServiceImpl service = new WalletCoreServiceImpl(
            accountMapper, transactionMapper, releaseMapper, freezeMapper, turnoverTaskService, existingMemberJdbcTemplate());
        stubAccount(accountMapper, transactionMapper, "promotion:PR1", "SC");
        WalletCreditBo bo = creditBo("PROMOTION", WalletReleaseMode.AFTER_TURNOVER.name());
        bo.setBusinessNo("PR1");
        bo.setIdempotencyKey("promotion:PR1");
        bo.setAmount(new BigDecimal("20"));
        bo.setFundPropertyCode("ACTIVITY_REWARD");
        bo.setTurnoverRequiredAmount(new BigDecimal("200"));
        bo.setGameScopeType("GAME");
        bo.setGameScopeValue("slot-001,slot-002");
        bo.setSourceId("activity-100");
        bo.setRuleSnapshot("{\"turnoverMultiplier\":10}");

        WalletTransaction transaction = service.credit(bo);

        assertEquals(WalletTransactionStatus.SUCCESS.name(), transaction.getStatus());
        assertEquals(new BigDecimal("200.000000"), transaction.getRequiredTurnover());
        verify(releaseMapper).insert(any(WalletRelease.class));
        verify(turnoverTaskService).createFromCredit(eq("000000"), eq(bo), eq(transaction), eq(new BigDecimal("20.000000")),
            eq(new BigDecimal("200.000000")), any());
    }

    @Test
    @Tag("local")
    void creditUsesRequestPolicyWithoutWalletRuleLookup() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        WalletReleaseMapper releaseMapper = mock(WalletReleaseMapper.class);
        WalletFreezeMapper freezeMapper = mock(WalletFreezeMapper.class);
        WalletCoreServiceImpl service = service(accountMapper, transactionMapper, releaseMapper, freezeMapper, existingMemberJdbcTemplate());
        stubAccount(accountMapper, transactionMapper, "promotion:PR1", "SC");
        WalletCreditBo bo = creditBo("PROMOTION", WalletReleaseMode.AFTER_TURNOVER.name());
        bo.setBusinessNo("PR1");
        bo.setIdempotencyKey("promotion:PR1");
        bo.setRequiredTurnover(new BigDecimal("100"));

        WalletTransaction transaction = service.credit(bo);

        assertEquals(WalletReleaseMode.AFTER_TURNOVER.name(), transaction.getReleaseMode());
        assertEquals(new BigDecimal("100.000000"), transaction.getRequiredTurnover());
        verify(releaseMapper).insert(any(WalletRelease.class));
    }

    @Test
    @Tag("local")
    void creditRejectsMissingMemberBeforeCreatingWalletData() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        WalletReleaseMapper releaseMapper = mock(WalletReleaseMapper.class);
        WalletFreezeMapper freezeMapper = mock(WalletFreezeMapper.class);
        WalletCoreServiceImpl service = service(accountMapper, transactionMapper, releaseMapper, freezeMapper, missingMemberJdbcTemplate());

        assertThrows(ServiceException.class, () -> service.credit(creditBo("MANUAL_ADJUST", WalletReleaseMode.IMMEDIATE.name())));

        verify(transactionMapper, never()).insert(any(WalletTransaction.class));
        verify(accountMapper, never()).insert(any(WalletAccount.class));
        verify(releaseMapper, never()).insert(any(WalletRelease.class));
    }

    @Test
    @Tag("local")
    void freezeRejectsMissingMemberBeforeCreatingWalletData() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        WalletReleaseMapper releaseMapper = mock(WalletReleaseMapper.class);
        WalletFreezeMapper freezeMapper = mock(WalletFreezeMapper.class);
        WalletCoreServiceImpl service = service(accountMapper, transactionMapper, releaseMapper, freezeMapper, missingMemberJdbcTemplate());

        assertThrows(ServiceException.class, () -> service.freeze(freezeBo()));

        verify(transactionMapper, never()).insert(any(WalletTransaction.class));
        verify(freezeMapper, never()).insert(any(WalletFreeze.class));
    }

    private static WalletCoreServiceImpl service(WalletAccountMapper accountMapper,
                                                 WalletTransactionMapper transactionMapper,
                                                 WalletReleaseMapper releaseMapper,
                                                 WalletFreezeMapper freezeMapper,
                                                 JdbcTemplate jdbcTemplate) {
        return new WalletCoreServiceImpl(
            accountMapper, transactionMapper, releaseMapper, freezeMapper, mock(IWalletTurnoverTaskService.class), jdbcTemplate);
    }

    private static void stubAccount(WalletAccountMapper accountMapper, WalletTransactionMapper transactionMapper,
                                    String idempotencyKey, String currencyCode) {
        WalletAccount account = new WalletAccount();
        account.setAvailableBalance(BigDecimal.ZERO);
        account.setFrozenBalance(BigDecimal.ZERO);
        when(transactionMapper.selectByIdempotencyKey(eq("000000"), eq(idempotencyKey))).thenReturn(null);
        when(accountMapper.selectByBizKeyForUpdate(eq("000000"), eq(1001L), eq(currencyCode))).thenReturn(account);
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

    private static WalletFreezeOperationBo freezeBo() {
        WalletFreezeOperationBo bo = new WalletFreezeOperationBo();
        bo.setMemberId(1001L);
        bo.setCurrencyCode("RC");
        bo.setAmount(new BigDecimal("5.000000"));
        bo.setSourceType("REDEMPTION");
        bo.setBusinessNo("RD1");
        bo.setIdempotencyKey("redemption:freeze:RD1");
        return bo;
    }

    private static JdbcTemplate existingMemberJdbcTemplate() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("000000"), eq(1001L))).thenReturn(1);
        return jdbcTemplate;
    }

    private static JdbcTemplate missingMemberJdbcTemplate() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("000000"), eq(1001L))).thenReturn(0);
        return jdbcTemplate;
    }
}
