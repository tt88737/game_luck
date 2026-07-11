package com.gameluck.wallet.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.gameluck.wallet.domain.WalletFreeze;
import com.gameluck.wallet.domain.WalletRelease;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletFreezeBo;
import com.gameluck.wallet.domain.bo.WalletReleaseBo;
import com.gameluck.wallet.domain.bo.WalletTransactionBo;
import com.gameluck.wallet.mapper.WalletFreezeMapper;
import com.gameluck.wallet.mapper.WalletReleaseMapper;
import com.gameluck.wallet.mapper.WalletTransactionMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WalletMemberNoQueryServiceTest {

    @Test
    @Tag("local")
    void transactionQueryCanFilterByMemberNo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), WalletTransaction.class);
        WalletTransactionMapper mapper = mock(WalletTransactionMapper.class);
        WalletTransactionServiceImpl service = new WalletTransactionServiceImpl(mapper, mock(JdbcTemplate.class));
        WalletTransactionBo bo = new WalletTransactionBo();
        bo.setMemberNo("GL000005");

        service.queryList(bo);

        ArgumentCaptor<LambdaQueryWrapper<WalletTransaction>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectVoList(captor.capture());
        assertMemberNoSql(captor.getValue().getSqlSegment(), "gl_wallet_transaction");
    }

    @Test
    @Tag("local")
    void releaseQueryCanFilterByMemberNo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), WalletRelease.class);
        WalletReleaseMapper mapper = mock(WalletReleaseMapper.class);
        WalletReleaseServiceImpl service = new WalletReleaseServiceImpl(mapper, mock(JdbcTemplate.class));
        WalletReleaseBo bo = new WalletReleaseBo();
        bo.setMemberNo("GL000005");

        service.queryList(bo);

        ArgumentCaptor<LambdaQueryWrapper<WalletRelease>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectVoList(captor.capture());
        assertMemberNoSql(captor.getValue().getSqlSegment(), "gl_wallet_release");
    }

    @Test
    @Tag("local")
    void freezeQueryCanFilterByMemberNo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), WalletFreeze.class);
        WalletFreezeMapper mapper = mock(WalletFreezeMapper.class);
        WalletFreezeServiceImpl service = new WalletFreezeServiceImpl(mapper, mock(JdbcTemplate.class));
        WalletFreezeBo bo = new WalletFreezeBo();
        bo.setMemberNo("GL000005");

        service.queryList(bo);

        ArgumentCaptor<LambdaQueryWrapper<WalletFreeze>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectVoList(captor.capture());
        assertMemberNoSql(captor.getValue().getSqlSegment(), "gl_wallet_freeze");
    }

    private void assertMemberNoSql(String sqlSegment, String tableName) {
        assertTrue(sqlSegment.contains("gl_member_profile"));
        assertTrue(sqlSegment.contains("member_no"));
        assertTrue(sqlSegment.contains(tableName));
    }
}
