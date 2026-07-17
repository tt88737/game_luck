package com.gameluck.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.gameluck.wallet.domain.WalletCurrency;
import com.gameluck.wallet.domain.bo.WalletCurrencyBo;
import com.gameluck.wallet.mapper.WalletCurrencyMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletCurrencyServiceImplTest {

    @Test
    @Tag("local")
    void updateByBoPersistsClientAbilityFlags() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), WalletCurrency.class);
        WalletCurrencyMapper mapper = mock(WalletCurrencyMapper.class);
        when(mapper.update(any(LambdaUpdateWrapper.class))).thenReturn(1);
        WalletCurrencyServiceImpl service = new WalletCurrencyServiceImpl(mapper);
        WalletCurrencyBo bo = new WalletCurrencyBo();
        bo.setId(1L);
        bo.setCurrencyName("Sweep Coin");
        bo.setScaleNum(6);
        bo.setEnabled("0");
        bo.setCreditEnabled("0");
        bo.setDebitEnabled("0");
        bo.setFreezeEnabled("0");
        bo.setDepositEnabled("0");
        bo.setWithdrawEnabled("0");
        bo.setExchangeEnabled("0");
        bo.setExchangeInEnabled("0");
        bo.setExchangeOutEnabled("0");
        bo.setPlayEnabled("0");
        bo.setNegativeAllowed("1");
        bo.setSortOrder(2);
        bo.setRemark("demo");

        service.updateByBo(bo);

        ArgumentCaptor<LambdaUpdateWrapper<WalletCurrency>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(mapper).update(captor.capture());
        String sqlSet = captor.getValue().getSqlSet();
        assertTrue(sqlSet.contains("deposit_enabled"));
        assertTrue(sqlSet.contains("exchange_in_enabled"));
        assertTrue(sqlSet.contains("exchange_out_enabled"));
        assertTrue(sqlSet.contains("play_enabled"));
    }
}
