package com.gameluck.game.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.game.domain.GameBetOrder;
import com.gameluck.game.enums.GameBetOrderStatus;
import com.gameluck.game.mapper.GameBetOrderMapper;
import com.gameluck.wallet.service.IWalletCoreService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GameBetOrderServiceImplTest {

    @Test
    @Tag("local")
    void cancelRejectsSettledOrderWithClearMessage() {
        GameBetOrderMapper mapper = mock(GameBetOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        GameBetOrderServiceImpl service = new GameBetOrderServiceImpl(mapper, walletCoreService);

        GameBetOrder order = new GameBetOrder();
        order.setId(1L);
        order.setStatus(GameBetOrderStatus.SETTLED.name());
        when(mapper.selectByIdForUpdate(1L)).thenReturn(order);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.cancel(1L));

        assertEquals("已结算订单不能取消退款", exception.getMessage());
        verifyNoInteractions(walletCoreService);
    }
}
