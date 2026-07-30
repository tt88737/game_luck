package com.gameluck.wallet.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.wallet.domain.WalletExchangeOrder;
import com.gameluck.wallet.domain.bo.WalletExchangeOrderBo;
import com.gameluck.wallet.domain.vo.WalletExchangeOrderVo;
import com.gameluck.wallet.mapper.WalletExchangeOrderMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WalletExchangeOrderAdminServiceImplTest {

    @Test
    @Tag("local")
    void queryPageListSupportsOperationalFilters() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), WalletExchangeOrder.class);
        WalletExchangeOrderMapper mapper = mock(WalletExchangeOrderMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(mapper.selectVoPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(new Page<WalletExchangeOrderVo>(1, 10));

        WalletExchangeOrderAdminServiceImpl service = new WalletExchangeOrderAdminServiceImpl(mapper, jdbcTemplate);
        WalletExchangeOrderBo bo = new WalletExchangeOrderBo();
        bo.setTenantId("000000");
        bo.setExchangeOrderNo("WE1001");
        bo.setMemberId(1001L);
        bo.setMemberNo("GL1001");
        bo.setFromCurrencyCode("GC");
        bo.setToCurrencyCode("SC");
        bo.setStatus("SUCCESS");
        bo.setDebitTransactionNo("WT-DEBIT");
        bo.setCreditTransactionNo("WT-CREDIT");
        bo.setBeginTime(new Date(1_000L));
        bo.setEndTime(new Date(2_000L));

        TableDataInfo<WalletExchangeOrderVo> result = service.queryPageList(bo, new PageQuery(1, 10));

        assertEquals(0, result.getTotal());
        ArgumentCaptor<LambdaQueryWrapper<WalletExchangeOrder>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectVoPage(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<WalletExchangeOrder> wrapper = wrapperCaptor.getValue();
        assertNotNull(wrapper);
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("tenant_id"));
        assertTrue(sqlSegment.contains("exchange_order_no"));
        assertTrue(sqlSegment.contains("member_id"));
        assertTrue(sqlSegment.contains("gl_member_profile"));
        assertTrue(sqlSegment.contains("member_no"));
        assertTrue(sqlSegment.contains("from_currency_code"));
        assertTrue(sqlSegment.contains("to_currency_code"));
        assertTrue(sqlSegment.contains("status"));
        assertTrue(sqlSegment.contains("debit_transaction_no"));
        assertTrue(sqlSegment.contains("credit_transaction_no"));
        assertTrue(sqlSegment.contains("create_time"));

        Map<String, Object> values = wrapper.getParamNameValuePairs();
        assertTrue(values.containsValue("000000"));
        assertTrue(values.containsValue("WE1001"));
        assertTrue(values.containsValue(1001L));
        assertTrue(values.containsValue("GL1001"));
        assertTrue(values.containsValue("GC"));
        assertTrue(values.containsValue("SC"));
        assertTrue(values.containsValue("SUCCESS"));
        assertTrue(values.containsValue("WT-DEBIT"));
        assertTrue(values.containsValue("WT-CREDIT"));
        assertTrue(values.containsValue(new Date(1_000L)));
        assertTrue(values.containsValue(new Date(2_000L)));
    }
}
