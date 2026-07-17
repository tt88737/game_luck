package com.gameluck.payment.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.gameluck.payment.domain.DepositOrder;
import com.gameluck.payment.domain.bo.DepositOrderBo;
import com.gameluck.payment.enums.DepositOrderStatus;
import com.gameluck.payment.mapper.DepositOrderMapper;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.service.IWalletCoreService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DepositOrderServiceImplTest {

    @Test
    @Tag("local")
    void queryListCanFilterByMemberNoUsingDepositOrderTable() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), DepositOrder.class);
        DepositOrderMapper mapper = mock(DepositOrderMapper.class);
        DepositOrderServiceImpl service = new DepositOrderServiceImpl(mapper, mock(IWalletCoreService.class), mock(JdbcTemplate.class));
        DepositOrderBo bo = new DepositOrderBo();
        bo.setMemberNo("GL000005");

        service.queryList(bo);

        ArgumentCaptor<LambdaQueryWrapper<DepositOrder>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectVoList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("gl_payment_deposit_order.tenant_id"));
        assertFalse(sqlSegment.contains("gl_deposit_order.tenant_id"));
    }

    @Test
    @Tag("local")
    void simulateSuccessCreditsDepositPrincipalWithTurnoverSnapshotFields() {
        DepositOrderMapper mapper = mock(DepositOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        DepositOrderServiceImpl service = new DepositOrderServiceImpl(mapper, walletCoreService, mock(JdbcTemplate.class));
        DepositOrder order = new DepositOrder();
        order.setId(100L);
        order.setTenantId("000000");
        order.setDepositOrderNo("DP202607130001");
        order.setMemberId(1001L);
        order.setCurrencyCode("SC");
        order.setAmount(new BigDecimal("50.000000"));
        order.setStatus(DepositOrderStatus.PENDING.name());
        order.setWalletIdempotencyKey("deposit:success:DP202607130001");
        when(mapper.selectByIdForUpdate(100L)).thenReturn(order);
        when(mapper.updateById(any(DepositOrder.class))).thenReturn(1);
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo("WT_DEPOSIT_1");
        transaction.setStatus(WalletTransactionStatus.SUCCESS.name());
        when(walletCoreService.credit(any())).thenReturn(transaction);

        service.simulateSuccess(100L);

        ArgumentCaptor<WalletCreditBo> creditCaptor = ArgumentCaptor.forClass(WalletCreditBo.class);
        verify(walletCoreService).credit(creditCaptor.capture());
        WalletCreditBo creditBo = creditCaptor.getValue();
        assertEquals("DEPOSIT_PRINCIPAL", creditBo.getFundPropertyCode());
        assertEquals(BigDecimal.ONE, creditBo.getTurnoverMultiplier());
        assertEquals("ALL", creditBo.getGameScopeType());
        assertEquals("100", creditBo.getSourceId());
    }
}
