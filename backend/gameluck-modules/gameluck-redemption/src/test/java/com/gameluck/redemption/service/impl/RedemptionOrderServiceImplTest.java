package com.gameluck.redemption.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.redemption.domain.RedemptionOrder;
import com.gameluck.redemption.enums.RedemptionOrderStatus;
import com.gameluck.redemption.mapper.RedemptionOrderMapper;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.service.IWalletCoreService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RedemptionOrderServiceImplTest {

    @Test
    @Tag("local")
    void rejectApprovedOrderDoesNotCallWalletAgain() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        RedemptionOrderServiceImpl service = new RedemptionOrderServiceImpl(mapper, walletCoreService);

        RedemptionOrder order = pendingOrder();
        order.setStatus(RedemptionOrderStatus.APPROVED.name());
        when(mapper.selectByIdForUpdate(1L)).thenReturn(order);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.reject(1L, "duplicate reject"));

        assertEquals("redemption.order.only.pending.allowed", exception.getMessage());
        verifyNoInteractions(walletCoreService);
    }

    @Test
    @Tag("local")
    void approveRejectedOrderDoesNotCallWalletAgain() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        RedemptionOrderServiceImpl service = new RedemptionOrderServiceImpl(mapper, walletCoreService);

        RedemptionOrder order = pendingOrder();
        order.setStatus(RedemptionOrderStatus.REJECTED.name());
        when(mapper.selectByIdForUpdate(1L)).thenReturn(order);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.approve(1L, "duplicate approve"));

        assertEquals("redemption.order.only.pending.allowed", exception.getMessage());
        verifyNoInteractions(walletCoreService);
    }

    @Test
    @Tag("local")
    void rejectBlankReasonDoesNotCallWallet() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        RedemptionOrderServiceImpl service = new RedemptionOrderServiceImpl(mapper, walletCoreService);

        RedemptionOrder order = pendingOrder();
        when(mapper.selectByIdForUpdate(1L)).thenReturn(order);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.reject(1L, "  "));

        assertEquals("redemption.audit.reject.reason.required", exception.getMessage());
        verifyNoInteractions(walletCoreService);
        verify(mapper, never()).updateById(any(RedemptionOrder.class));
    }

    @Test
    @Tag("local")
    void approvePendingOrderSettlesFreezeAndWritesAuditFields() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        RedemptionOrderServiceImpl service = new RedemptionOrderServiceImpl(mapper, walletCoreService);

        RedemptionOrder order = pendingOrder();
        when(mapper.selectByIdForUpdate(1L)).thenReturn(order);
        when(walletCoreService.settle(any())).thenReturn(successTransaction("WT_SETTLE_1"));

        service.approve(1L, "approved by ops");

        assertEquals(RedemptionOrderStatus.APPROVED.name(), order.getStatus());
        assertEquals("WT_SETTLE_1", order.getSettleWalletTransactionNo());
        assertEquals("approved by ops", order.getAuditReason());
        assertNull(order.getFailReason());
        assertNotNull(order.getAuditTime());
        assertNotNull(order.getUpdateTime());
        verify(mapper).updateById(order);
    }

    @Test
    @Tag("local")
    void rejectPendingOrderReleasesFreezeAndWritesAuditFields() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        RedemptionOrderServiceImpl service = new RedemptionOrderServiceImpl(mapper, walletCoreService);

        RedemptionOrder order = pendingOrder();
        when(mapper.selectByIdForUpdate(1L)).thenReturn(order);
        when(walletCoreService.unfreeze(any())).thenReturn(successTransaction("WT_RELEASE_1"));

        service.reject(1L, "account mismatch");

        assertEquals(RedemptionOrderStatus.REJECTED.name(), order.getStatus());
        assertEquals("WT_RELEASE_1", order.getReleaseWalletTransactionNo());
        assertEquals("account mismatch", order.getAuditReason());
        assertNull(order.getFailReason());
        assertNotNull(order.getAuditTime());
        assertNotNull(order.getUpdateTime());
        verify(mapper).updateById(order);
    }

    @Test
    @Tag("local")
    void approveWalletFailureDoesNotUpdateOrderToApproved() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        RedemptionOrderServiceImpl service = new RedemptionOrderServiceImpl(mapper, walletCoreService);

        RedemptionOrder order = pendingOrder();
        when(mapper.selectByIdForUpdate(1L)).thenReturn(order);
        when(walletCoreService.settle(any())).thenReturn(failedTransaction());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.approve(1L, "approve"));

        assertEquals("redemption.wallet.operation.fail", exception.getMessage());
        assertEquals(RedemptionOrderStatus.PENDING.name(), order.getStatus());
        verify(mapper, never()).updateById(any(RedemptionOrder.class));
    }

    @Test
    @Tag("local")
    void rejectWalletFailureDoesNotUpdateOrderToRejected() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        RedemptionOrderServiceImpl service = new RedemptionOrderServiceImpl(mapper, walletCoreService);

        RedemptionOrder order = pendingOrder();
        when(mapper.selectByIdForUpdate(1L)).thenReturn(order);
        when(walletCoreService.unfreeze(any())).thenReturn(failedTransaction());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.reject(1L, "reject"));

        assertEquals("redemption.wallet.operation.fail", exception.getMessage());
        assertEquals(RedemptionOrderStatus.PENDING.name(), order.getStatus());
        verify(mapper, never()).updateById(any(RedemptionOrder.class));
    }

    private RedemptionOrder pendingOrder() {
        RedemptionOrder order = new RedemptionOrder();
        order.setId(1L);
        order.setRedemptionOrderNo("RD_TEST_1");
        order.setMemberId(1001L);
        order.setCurrencyCode("RC");
        order.setAmount(new BigDecimal("1.000000"));
        order.setStatus(RedemptionOrderStatus.PENDING.name());
        order.setFreezeNo("WF_TEST_1");
        order.setSettleIdempotencyKey("redemption:settle:RD_TEST_1");
        order.setReleaseIdempotencyKey("redemption:release:RD_TEST_1");
        return order;
    }

    private WalletTransaction successTransaction(String transactionNo) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo(transactionNo);
        transaction.setStatus(WalletTransactionStatus.SUCCESS.name());
        return transaction;
    }

    private WalletTransaction failedTransaction() {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo("WT_FAILED_1");
        transaction.setStatus(WalletTransactionStatus.FAILED.name());
        transaction.setFailReason("wallet failed");
        return transaction;
    }
}
