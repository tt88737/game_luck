package com.gameluck.redemption.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.redemption.domain.RedemptionOrder;
import com.gameluck.redemption.enums.RedemptionOrderStatus;
import com.gameluck.redemption.mapper.RedemptionOrderMapper;
import com.gameluck.wallet.service.IWalletCoreService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RedemptionOrderServiceImplTest {

    @Test
    @Tag("local")
    void rejectApprovedOrderDoesNotCallWalletAgain() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        RedemptionOrderServiceImpl service = new RedemptionOrderServiceImpl(mapper, walletCoreService);

        RedemptionOrder order = new RedemptionOrder();
        order.setId(1L);
        order.setStatus(RedemptionOrderStatus.APPROVED.name());
        when(mapper.selectByIdForUpdate(1L)).thenReturn(order);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.reject(1L, "duplicate reject"));

        assertEquals("redemption.order.only.pending.allowed", exception.getMessage());
        verifyNoInteractions(walletCoreService);
    }
}
