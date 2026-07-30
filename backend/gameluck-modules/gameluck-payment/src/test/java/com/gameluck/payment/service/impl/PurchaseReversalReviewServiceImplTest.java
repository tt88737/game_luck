package com.gameluck.payment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.satoken.utils.LoginHelper;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.member.mapper.MemberProfileMapper;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.domain.PurchaseReversal;
import com.gameluck.payment.domain.PurchaseReversalItem;
import com.gameluck.payment.domain.PurchaseReversalReviewLog;
import com.gameluck.payment.domain.bo.PurchaseReversalReviewBo;
import com.gameluck.payment.domain.bo.PurchaseReversalReviewActionBo;
import com.gameluck.payment.domain.vo.PurchaseReversalReviewDetailVo;
import com.gameluck.payment.domain.vo.PurchaseReversalReviewActionResultVo;
import com.gameluck.payment.service.IPurchaseReversalReviewService;
import com.gameluck.payment.mapper.PurchaseOrderGrantSnapshotMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.mapper.PurchasePaymentEventMapper;
import com.gameluck.payment.mapper.PurchaseReversalItemMapper;
import com.gameluck.payment.mapper.PurchaseReversalMapper;
import com.gameluck.payment.mapper.PurchaseReversalReviewLogMapper;
import com.gameluck.wallet.domain.vo.WalletBatchDebitLineResult;
import com.gameluck.wallet.domain.vo.WalletBatchDebitPreviewLineResult;
import com.gameluck.wallet.domain.vo.WalletBatchDebitPreviewResult;
import com.gameluck.wallet.domain.vo.WalletBatchDebitResult;
import com.gameluck.wallet.service.IWalletCoreService;
import com.gameluck.wallet.service.IWalletTurnoverTaskService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.apache.ibatis.annotations.Update;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseReversalReviewServiceImplTest {

    @Test
    @Tag("local")
    void exposesTenantSafeListAndDetailContract() throws Exception {
        Method list = IPurchaseReversalReviewService.class.getMethod(
            "queryPageList", PurchaseReversalReviewBo.class, PageQuery.class);
        Method detail = IPurchaseReversalReviewService.class.getMethod("queryByReversalNo", String.class);

        assertNotNull(list);
        assertEquals(PurchaseReversalReviewDetailVo.class, detail.getReturnType());
    }

    @Test
    @Tag("local")
    void exposesRetryAndLossAcceptanceContracts() throws Exception {
        Method retry = IPurchaseReversalReviewService.class.getMethod(
            "retry", String.class, PurchaseReversalReviewActionBo.class);
        Method loss = IPurchaseReversalReviewService.class.getMethod(
            "acceptLoss", String.class, PurchaseReversalReviewActionBo.class);
        assertEquals(PurchaseReversalReviewActionResultVo.class, retry.getReturnType());
        assertEquals(PurchaseReversalReviewActionResultVo.class, loss.getReturnType());
    }

    @Test
    @Tag("local")
    void guardedFinalizationPersistsSuccessfulRetryMetadata() throws Exception {
        Method method = java.util.Arrays.stream(PurchaseReversalMapper.class.getMethods())
            .filter(candidate -> candidate.getName().equals("finalizeDisposition"))
            .findFirst().orElseThrow();
        String sql = method.getAnnotation(Update.class).value()[0];

        org.junit.jupiter.api.Assertions.assertTrue(sql.contains("retry_count = #{retryCount}"));
        org.junit.jupiter.api.Assertions.assertTrue(sql.contains("last_retry_time = #{lastRetryTime}"));
    }

    @Test
    @Tag("local")
    void retryKeepsCasePendingAndWritesSnapshotWhenAnyCurrencyIsInsufficient() {
        Fixture f = fixture();
        when(f.wallet.batchDebit(any())).thenReturn(batchResult("REVIEW_REQUIRED",
            debitLine("GC", "100", "120", "0", "0"), debitLine("SC", "10", "3", "0", "7")));

        try (StaticContext ignored = staticContext()) {
            PurchaseReversalReviewActionResultVo result = f.service.retry("PR1", action("retry-1", "balance checked"));

            assertFalse(result.isCompleted());
            assertEquals("PENDING_REVIEW", result.getDispositionStatus());
            verify(f.itemMapper, times(2)).updateById(any(PurchaseReversalItem.class));
            verify(f.reversalMapper).updateById(any(PurchaseReversal.class));
            verify(f.turnover, never()).cancelPendingByPurchase(anyString(), any(), anyString(), anyString(), any());
            verify(f.logMapper).insert(any(PurchaseReversalReviewLog.class));
        }
    }

    @Test
    @Tag("local")
    void retryCompletesEveryCurrencyAndFinalizesRefund() {
        Fixture f = fixture();
        when(f.wallet.batchDebit(any())).thenReturn(batchResult("COMPLETED",
            debitLine("GC", "100", "120", "100", "0"), debitLine("SC", "10", "20", "10", "0")));

        try (StaticContext ignored = staticContext()) {
            PurchaseReversalReviewActionResultVo result = f.service.retry("PR1", action("retry-2", null));

            assertEquals("RETRY_COMPLETED", result.getOperationType());
            verify(f.reversalMapper).finalizeDisposition(anyString(), anyString(), anyString(),
                org.mockito.ArgumentMatchers.eq("RECOVERY_COMPLETED"), org.mockito.ArgumentMatchers.eq("COMPLETED"),
                any(), anyString(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.eq(1), any());
            verify(f.turnover).cancelPendingByPurchase(anyString(), any(), anyString(), anyString(), any());
            ArgumentCaptor<PurchaseOrder> order = ArgumentCaptor.forClass(PurchaseOrder.class);
            verify(f.orderMapper).updateById(order.capture());
            assertEquals("REFUNDED", order.getValue().getStatus());
        }
    }

    @Test
    @Tag("local")
    void acceptLossRefreshesShortfallsWithoutDebitOrTurnoverCancellation() {
        Fixture f = fixture();
        WalletBatchDebitPreviewResult preview = new WalletBatchDebitPreviewResult();
        preview.setSufficient(false);
        preview.setLines(List.of(previewLine("GC", "100", "80", "20"), previewLine("SC", "10", "2", "8")));
        when(f.wallet.previewBatchDebit(any())).thenReturn(preview);

        try (StaticContext ignored = staticContext()) {
            PurchaseReversalReviewActionResultVo result = f.service.acceptLoss("PR1", action("loss-1", "provider evidence"));

            assertEquals("LOSS_ACCEPTED", result.getDispositionStatus());
            verify(f.wallet, never()).batchDebit(any());
            verify(f.turnover, never()).cancelPendingByPurchase(anyString(), any(), anyString(), anyString(), any());
            verify(f.orderMapper, never()).updateById(any(PurchaseOrder.class));
            verify(f.reversalMapper).finalizeDisposition(anyString(), anyString(), anyString(),
                org.mockito.ArgumentMatchers.eq("LOSS_ACCEPTED"), org.mockito.ArgumentMatchers.eq("REVIEW_REQUIRED"),
                any(), anyString(), org.mockito.ArgumentMatchers.eq("provider evidence"), any(), any(), any(),
                org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.isNull());
        }
    }

    @Test
    @Tag("local")
    void terminalCaseRejectsDifferentRequestWithoutWalletSideEffects() {
        Fixture f = fixture();
        PurchaseReversal terminal = reversal();
        terminal.setDispositionStatus("LOSS_ACCEPTED");
        when(f.reversalMapper.selectByReversalNoForUpdate("000000", "PR1")).thenReturn(terminal);

        try (StaticContext ignored = staticContext()) {
            assertThrows(RuntimeException.class, () -> f.service.retry("PR1", action("new-key", null)));
            verify(f.wallet, never()).batchDebit(any());
            verify(f.logMapper, never()).insert(any(PurchaseReversalReviewLog.class));
        }
    }

    private static Fixture fixture() {
        PurchaseReversalMapper reversalMapper = mock(PurchaseReversalMapper.class);
        PurchaseReversalItemMapper itemMapper = mock(PurchaseReversalItemMapper.class);
        PurchaseReversalReviewLogMapper logMapper = mock(PurchaseReversalReviewLogMapper.class);
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        PurchaseOrderGrantSnapshotMapper snapshotMapper = mock(PurchaseOrderGrantSnapshotMapper.class);
        PurchasePaymentEventMapper eventMapper = mock(PurchasePaymentEventMapper.class);
        MemberProfileMapper memberMapper = mock(MemberProfileMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        IWalletCoreService wallet = mock(IWalletCoreService.class);
        IWalletTurnoverTaskService turnover = mock(IWalletTurnoverTaskService.class);
        PurchaseReversal reversal = reversal();
        when(reversalMapper.selectByReversalNoForUpdate("000000", "PR1")).thenReturn(reversal);
        when(reversalMapper.selectByReversalNo("000000", "PR1")).thenReturn(reversal);
        when(reversalMapper.finalizeDisposition(anyString(), anyString(), anyString(), anyString(), anyString(),
            any(), anyString(), any(), any(), any(), any(), any(), any())).thenAnswer(invocation -> {
                reversal.setDispositionStatus(invocation.getArgument(3));
                reversal.setStatus(invocation.getArgument(4));
                return 1;
            });
        when(itemMapper.selectByReversalNo("000000", "PR1")).thenReturn(items());
        when(logMapper.selectByRequestKey(anyString(), anyString())).thenReturn(null);
        when(logMapper.selectByReversalNo("000000", "PR1")).thenReturn(Collections.emptyList());
        when(orderMapper.selectByOrderNoForUpdate("000000", "PO1")).thenReturn(order());
        when(snapshotMapper.selectByPurchaseOrderNo("000000", "PO1")).thenReturn(Collections.emptyList());
        when(eventMapper.selectByPurchaseOrderNo("000000", "PO1")).thenReturn(Collections.emptyList());
        PurchaseReversalReviewServiceImpl service = new PurchaseReversalReviewServiceImpl(reversalMapper, itemMapper,
            logMapper, orderMapper, snapshotMapper, eventMapper, memberMapper, jdbcTemplate, wallet, turnover, new ObjectMapper());
        return new Fixture(service, reversalMapper, itemMapper, logMapper, orderMapper, wallet, turnover);
    }

    private static PurchaseReversal reversal() {
        PurchaseReversal value = new PurchaseReversal();
        value.setId(1L); value.setTenantId("000000"); value.setReversalNo("PR1"); value.setPurchaseOrderNo("PO1");
        value.setMemberId(1001L); value.setReversalType("REFUND"); value.setStatus("REVIEW_REQUIRED");
        value.setDispositionStatus("PENDING_REVIEW"); value.setRetryCount(0); value.setReviewReason("shortfall");
        return value;
    }

    private static PurchaseOrder order() {
        PurchaseOrder value = new PurchaseOrder();
        value.setId(2L); value.setTenantId("000000"); value.setPurchaseOrderNo("PO1"); value.setMemberId(1001L);
        value.setStatus("REFUND_REVIEW"); return value;
    }

    private static List<PurchaseReversalItem> items() {
        return List.of(item(11L, "GC", "100"), item(12L, "SC", "10"));
    }

    private static PurchaseReversalItem item(Long id, String currency, String required) {
        PurchaseReversalItem value = new PurchaseReversalItem();
        value.setId(id); value.setTenantId("000000"); value.setReversalNo("PR1"); value.setPurchaseOrderNo("PO1");
        value.setMemberId(1001L); value.setCurrencyCode(currency); value.setRequiredAmount(new BigDecimal(required));
        value.setRecoveredAmount(BigDecimal.ZERO); value.setShortfallAmount(new BigDecimal(required)); value.setStatus("REVIEW_REQUIRED");
        return value;
    }

    private static PurchaseReversalReviewActionBo action(String key, String note) {
        PurchaseReversalReviewActionBo value = new PurchaseReversalReviewActionBo(); value.setRequestKey(key); value.setReviewNote(note); return value;
    }

    private static WalletBatchDebitResult batchResult(String status, WalletBatchDebitLineResult... lines) {
        WalletBatchDebitResult value = new WalletBatchDebitResult(); value.setStatus(status); value.setLines(List.of(lines)); return value;
    }

    private static WalletBatchDebitLineResult debitLine(String currency, String required, String available, String recovered, String shortfall) {
        WalletBatchDebitLineResult value = new WalletBatchDebitLineResult(); value.setCurrencyCode(currency);
        value.setRequiredAmount(new BigDecimal(required)); value.setAvailableAmount(new BigDecimal(available));
        value.setRecoveredAmount(new BigDecimal(recovered)); value.setShortfallAmount(new BigDecimal(shortfall));
        value.setWalletTransactionNo("0".equals(shortfall) ? "WT-" + currency : null); return value;
    }

    private static WalletBatchDebitPreviewLineResult previewLine(String currency, String required, String available, String shortfall) {
        WalletBatchDebitPreviewLineResult value = new WalletBatchDebitPreviewLineResult(); value.setCurrencyCode(currency);
        value.setRequiredAmount(new BigDecimal(required)); value.setAvailableAmount(new BigDecimal(available));
        value.setShortfallAmount(new BigDecimal(shortfall)); return value;
    }

    private static StaticContext staticContext() {
        MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class);
        MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class);
        tenant.when(TenantHelper::getTenantId).thenReturn("000000");
        login.when(LoginHelper::getUserId).thenReturn(99L);
        login.when(LoginHelper::getUsername).thenReturn("reviewer");
        return new StaticContext(tenant, login);
    }

    private record Fixture(PurchaseReversalReviewServiceImpl service, PurchaseReversalMapper reversalMapper,
                           PurchaseReversalItemMapper itemMapper, PurchaseReversalReviewLogMapper logMapper,
                           PurchaseOrderMapper orderMapper, IWalletCoreService wallet, IWalletTurnoverTaskService turnover) { }

    private record StaticContext(MockedStatic<TenantHelper> tenant, MockedStatic<LoginHelper> login) implements AutoCloseable {
        @Override public void close() { login.close(); tenant.close(); }
    }
}
