package com.gameluck.wallet.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.wallet.domain.WalletAccount;
import com.gameluck.wallet.domain.WalletFreeze;
import com.gameluck.wallet.domain.WalletRelease;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.domain.bo.WalletBatchDebitBo;
import com.gameluck.wallet.domain.bo.WalletBatchDebitLineBo;
import com.gameluck.wallet.domain.bo.WalletFreezeOperationBo;
import com.gameluck.wallet.domain.bo.WalletTurnoverBo;
import com.gameluck.wallet.domain.vo.WalletBatchDebitResult;
import com.gameluck.wallet.domain.vo.WalletBatchDebitPreviewResult;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

class WalletCoreServiceImplTest {

    @Test
    @Tag("local")
    void previewBatchDebitDoesNotWriteWhenEveryBalanceIsSufficient() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        WalletCoreServiceImpl service = service(accountMapper, transactionMapper, mock(WalletReleaseMapper.class),
            mock(WalletFreezeMapper.class), existingMemberJdbcTemplate());
        when(accountMapper.selectByBizKeyForUpdate("000000", 1001L, "GC")).thenReturn(account("20"));
        when(accountMapper.selectByBizKeyForUpdate("000000", 1001L, "SC")).thenReturn(account("30"));

        WalletBatchDebitPreviewResult result = service.previewBatchDebit(batchDebitBo());

        assertEquals(true, result.isSufficient());
        assertEquals(2, result.getLines().size());
        verify(accountMapper, never()).updateById(any(WalletAccount.class));
        verify(transactionMapper, never()).insert(any(WalletTransaction.class));
    }

    @Test
    @Tag("local")
    void previewBatchDebitReturnsEveryAvailableAndShortfallWithoutWrites() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        WalletCoreServiceImpl service = service(accountMapper, transactionMapper, mock(WalletReleaseMapper.class),
            mock(WalletFreezeMapper.class), existingMemberJdbcTemplate());
        when(accountMapper.selectByBizKeyForUpdate("000000", 1001L, "GC")).thenReturn(account("20"));
        when(accountMapper.selectByBizKeyForUpdate("000000", 1001L, "SC")).thenReturn(account("4"));

        WalletBatchDebitPreviewResult result = service.previewBatchDebit(batchDebitBo());

        assertEquals(false, result.isSufficient());
        assertEquals(new BigDecimal("6.000000"), result.getLines().get(1).getShortfallAmount());
        verify(accountMapper, never()).updateById(any(WalletAccount.class));
        verify(transactionMapper, never()).insert(any(WalletTransaction.class));
    }

    @Test
    @Tag("local")
    void batchDebitDebitsEveryCurrencyWhenAllBalancesAreSufficient() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        WalletCoreServiceImpl service = service(accountMapper, transactionMapper, mock(WalletReleaseMapper.class),
            mock(WalletFreezeMapper.class), existingMemberJdbcTemplate());
        WalletAccount gc = account("20");
        WalletAccount sc = account("30");
        when(accountMapper.selectByBizKeyForUpdate("000000", 1001L, "GC")).thenReturn(gc);
        when(accountMapper.selectByBizKeyForUpdate("000000", 1001L, "SC")).thenReturn(sc);

        WalletBatchDebitResult result = service.batchDebit(batchDebitBo());

        assertEquals("COMPLETED", result.getStatus());
        assertEquals(new BigDecimal("15.000000"), gc.getAvailableBalance());
        assertEquals(new BigDecimal("20.000000"), sc.getAvailableBalance());
        assertEquals(2, result.getLines().size());
        var order = inOrder(accountMapper);
        order.verify(accountMapper).selectByBizKeyForUpdate("000000", 1001L, "GC");
        order.verify(accountMapper).selectByBizKeyForUpdate("000000", 1001L, "SC");
        verify(transactionMapper, org.mockito.Mockito.times(2)).insert(any(WalletTransaction.class));
    }

    @Test
    @Tag("local")
    void batchDebitReturnsReviewWithoutWritesWhenOneCurrencyIsInsufficient() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        WalletCoreServiceImpl service = service(accountMapper, transactionMapper, mock(WalletReleaseMapper.class),
            mock(WalletFreezeMapper.class), existingMemberJdbcTemplate());
        when(accountMapper.selectByBizKeyForUpdate("000000", 1001L, "GC")).thenReturn(account("20"));
        when(accountMapper.selectByBizKeyForUpdate("000000", 1001L, "SC")).thenReturn(account("4"));

        WalletBatchDebitResult result = service.batchDebit(batchDebitBo());

        assertEquals("REVIEW_REQUIRED", result.getStatus());
        assertEquals(new BigDecimal("6.000000"), result.getLines().get(1).getShortfallAmount());
        verify(accountMapper, never()).updateById(any(WalletAccount.class));
        verify(transactionMapper, never()).insert(any(WalletTransaction.class));
    }

    @Test
    @Tag("local")
    void batchDebitTreatsMissingAccountAsFullShortfall() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        WalletCoreServiceImpl service = service(accountMapper, transactionMapper, mock(WalletReleaseMapper.class),
            mock(WalletFreezeMapper.class), existingMemberJdbcTemplate());
        when(accountMapper.selectByBizKeyForUpdate("000000", 1001L, "GC")).thenReturn(null);
        when(accountMapper.selectByBizKeyForUpdate("000000", 1001L, "SC")).thenReturn(account("30"));

        WalletBatchDebitResult result = service.batchDebit(batchDebitBo());

        assertEquals("REVIEW_REQUIRED", result.getStatus());
        assertEquals(new BigDecimal("5.000000"), result.getLines().get(0).getShortfallAmount());
        verify(accountMapper, never()).updateById(any(WalletAccount.class));
        verify(transactionMapper, never()).insert(any(WalletTransaction.class));
    }

    @Test
    @Tag("local")
    void batchDebitReplayReturnsOriginalTransactions() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        WalletCoreServiceImpl service = service(accountMapper, transactionMapper, mock(WalletReleaseMapper.class),
            mock(WalletFreezeMapper.class), existingMemberJdbcTemplate());
        WalletTransaction gc = successfulDebit("purchase-reversal:RV1:GC", "GC", "WT-GC", "5");
        WalletTransaction sc = successfulDebit("purchase-reversal:RV1:SC", "SC", "WT-SC", "10");
        when(transactionMapper.selectByIdempotencyKey("000000", "purchase-reversal:RV1:GC")).thenReturn(gc);
        when(transactionMapper.selectByIdempotencyKey("000000", "purchase-reversal:RV1:SC")).thenReturn(sc);

        WalletBatchDebitResult result = service.batchDebit(batchDebitBo());

        assertEquals("COMPLETED", result.getStatus());
        assertEquals("WT-GC", result.getLines().get(0).getWalletTransactionNo());
        assertEquals("WT-SC", result.getLines().get(1).getWalletTransactionNo());
        verify(accountMapper, never()).selectByBizKeyForUpdate(anyString(), any(), anyString());
        verify(transactionMapper, never()).insert(any(WalletTransaction.class));
    }

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
    void addValidTurnoverSynchronizesTurnoverTasksWhenReleaseProgresses() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        WalletReleaseMapper releaseMapper = mock(WalletReleaseMapper.class);
        WalletFreezeMapper freezeMapper = mock(WalletFreezeMapper.class);
        IWalletTurnoverTaskService turnoverTaskService = mock(IWalletTurnoverTaskService.class);
        WalletCoreServiceImpl service = new WalletCoreServiceImpl(
            accountMapper, transactionMapper, releaseMapper, freezeMapper, turnoverTaskService, existingMemberJdbcTemplate());
        WalletRelease release = new WalletRelease();
        release.setAmount(new BigDecimal("1.000000"));
        release.setRequiredTurnover(new BigDecimal("10.000000"));
        release.setCompletedTurnover(BigDecimal.ZERO);
        when(transactionMapper.selectByIdempotencyKey(eq("000000"), eq("turnover:PO1"))).thenReturn(null);
        when(releaseMapper.selectLockedByMemberForUpdate(eq("000000"), eq(1001L), eq("SC"), anyString()))
            .thenReturn(List.of(release));

        int releasedCount = service.addValidTurnover(turnoverBo());

        assertEquals(1, releasedCount);
        verify(turnoverTaskService).applyValidTurnover(eq("000000"), eq(turnoverBo().getMemberId()),
            eq(turnoverBo().getCurrencyCode()), eq(turnoverBo().getValidTurnoverAmount()), any());
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

    private static WalletTurnoverBo turnoverBo() {
        WalletTurnoverBo bo = new WalletTurnoverBo();
        bo.setMemberId(1001L);
        bo.setCurrencyCode("SC");
        bo.setSourceType("GAME_BET");
        bo.setBusinessNo("PO1");
        bo.setIdempotencyKey("turnover:PO1");
        bo.setValidTurnoverAmount(new BigDecimal("10.000000"));
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

    private static WalletBatchDebitBo batchDebitBo() {
        WalletBatchDebitBo bo = new WalletBatchDebitBo();
        bo.setTenantId("000000");
        bo.setMemberId(1001L);
        bo.setBusinessNo("RV1");
        bo.setSourceType("PURCHASE_REVERSAL");
        bo.setRemark("Purchase reversal RV1");
        bo.setLines(List.of(batchLine("sc", "10", "purchase-reversal:RV1:SC"),
            batchLine("gc", "5", "purchase-reversal:RV1:GC")));
        return bo;
    }

    private static WalletBatchDebitLineBo batchLine(String currencyCode, String amount, String idempotencyKey) {
        WalletBatchDebitLineBo line = new WalletBatchDebitLineBo();
        line.setCurrencyCode(currencyCode);
        line.setAmount(new BigDecimal(amount));
        line.setIdempotencyKey(idempotencyKey);
        return line;
    }

    private static WalletAccount account(String availableBalance) {
        WalletAccount account = new WalletAccount();
        account.setAvailableBalance(new BigDecimal(availableBalance));
        account.setFrozenBalance(BigDecimal.ZERO);
        return account;
    }

    private static WalletTransaction successfulDebit(String idempotencyKey, String currencyCode,
                                                      String transactionNo, String amount) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setCurrencyCode(currencyCode);
        transaction.setTransactionNo(transactionNo);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setBalanceBefore(new BigDecimal("30.000000"));
        transaction.setStatus(WalletTransactionStatus.SUCCESS.name());
        transaction.setRequestHash(DigestUtil.sha256Hex(String.join("|", "000000", idempotencyKey, "1001",
            currencyCode, "DEBIT", "PURCHASE_REVERSAL", "RV1", new BigDecimal(amount).setScale(6).toPlainString())));
        return transaction;
    }
}
