package com.gameluck.payment.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.gameluck.payment.domain.DepositOrder;
import com.gameluck.payment.domain.bo.DepositOrderBo;
import com.gameluck.payment.mapper.DepositOrderMapper;
import com.gameluck.wallet.service.IWalletCoreService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
}
