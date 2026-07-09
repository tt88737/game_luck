package com.gameluck.redemption.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.redemption.client.domain.bo.ClientRedemptionRequestBo;
import com.gameluck.redemption.client.domain.vo.ClientRedemptionVo;
import com.gameluck.redemption.domain.RedemptionOrder;
import com.gameluck.redemption.domain.bo.RedemptionOrderBo;
import com.gameluck.redemption.mapper.RedemptionOrderMapper;
import com.gameluck.redemption.service.IRedemptionOrderService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientRedemptionServiceTest {

    @Test
    @Tag("local")
    void listReturnsCurrentMemberOrdersOnly() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IRedemptionOrderService orderService = mock(IRedemptionOrderService.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientRedemptionService service = new ClientRedemptionService(mapper, orderService, tokenService);
        when(mapper.selectClientOrders("000000", 1001L, 0, 20)).thenReturn(List.of(order()));

        List<ClientRedemptionVo> result = service.redemptions("Bearer " + tokenService.issue(1001L));

        assertEquals(1, result.size());
        assertEquals("RD1001", result.get(0).getOrderNo());
        assertEquals("PENDING", result.get(0).getStatus());
    }

    @Test
    @Tag("local")
    void requestCreatesScRedemptionForCurrentMember() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IRedemptionOrderService orderService = mock(IRedemptionOrderService.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientRedemptionService service = new ClientRedemptionService(mapper, orderService, tokenService);
        when(orderService.insertByBo(any(RedemptionOrderBo.class))).thenReturn(true);
        ClientRedemptionRequestBo bo = new ClientRedemptionRequestBo();
        bo.setCurrencyCode("SC");
        bo.setAmount(new BigDecimal("1.00"));

        ClientRedemptionVo result = service.request("Bearer " + tokenService.issue(1001L), bo);

        assertEquals("SC", result.getCurrencyCode());
        assertEquals("1.00", result.getAmount());
        assertEquals("PENDING", result.getStatus());
        verify(orderService).insertByBo(any(RedemptionOrderBo.class));
    }

    @Test
    @Tag("local")
    void requestRejectsUnsupportedCurrency() {
        ClientRedemptionService service = new ClientRedemptionService(
            mock(RedemptionOrderMapper.class), mock(IRedemptionOrderService.class), new ClientTokenService());
        ClientRedemptionRequestBo bo = new ClientRedemptionRequestBo();
        bo.setCurrencyCode("GC");
        bo.setAmount(new BigDecimal("1.00"));

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.request("Bearer " + new ClientTokenService().issue(1001L), bo));

        assertEquals("client.redemption.currency.unsupported", exception.getMessage());
    }

    private RedemptionOrder order() {
        RedemptionOrder order = new RedemptionOrder();
        order.setId(1L);
        order.setRedemptionOrderNo("RD1001");
        order.setCurrencyCode("SC");
        order.setAmount(new BigDecimal("1.00"));
        order.setStatus("PENDING");
        order.setFreezeNo("WF1001");
        order.setCreateTime(new Date());
        return order;
    }
}
