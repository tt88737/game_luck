package com.gameluck.wallet.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.gameluck.wallet.domain.WalletAccount;
import com.gameluck.wallet.domain.bo.WalletAccountBo;
import com.gameluck.wallet.mapper.WalletAccountMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WalletAccountServiceImplTest {

    @Test
    @Tag("local")
    void queryListCanFilterByMemberNo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), WalletAccount.class);
        WalletAccountMapper mapper = mock(WalletAccountMapper.class);
        WalletAccountServiceImpl service = new WalletAccountServiceImpl(mapper, mock(JdbcTemplate.class));
        WalletAccountBo bo = new WalletAccountBo();
        bo.setMemberNo("MB1001");

        service.queryList(bo);

        ArgumentCaptor<LambdaQueryWrapper<WalletAccount>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectVoList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("gl_member_profile"));
        assertTrue(sqlSegment.contains("member_no"));
    }
}
