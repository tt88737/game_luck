package com.gameluck.wallet.service.impl;

import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.WalletTurnoverTask;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.mapper.WalletTurnoverTaskMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletTurnoverTaskServiceImplTest {

    @Test
    @Tag("local")
    void createFromCreditPersistsImmutableTurnoverSnapshot() {
        WalletTurnoverTaskMapper mapper = mock(WalletTurnoverTaskMapper.class);
        when(mapper.insert(any(WalletTurnoverTask.class))).thenReturn(1);
        WalletTurnoverTaskServiceImpl service = new WalletTurnoverTaskServiceImpl(mapper);
        WalletCreditBo bo = new WalletCreditBo();
        bo.setMemberId(1001L);
        bo.setCurrencyCode("SC");
        bo.setSourceType("PROMOTION");
        bo.setSourceId("activity-100");
        bo.setBusinessNo("PR1");
        bo.setFundPropertyCode("ACTIVITY_REWARD");
        bo.setGameScopeType("GAME");
        bo.setGameScopeValue("slot-001,slot-002");
        bo.setRuleSnapshot("{\"turnoverMultiplier\":10}");
        bo.setTurnoverExpireTime(new Date(1893456000000L));
        bo.setRemark("campaign reward");
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo("WT100");

        service.createFromCredit("000000", bo, transaction, new BigDecimal("20"), new BigDecimal("200"), new Date(1893369600000L));

        ArgumentCaptor<WalletTurnoverTask> captor = ArgumentCaptor.forClass(WalletTurnoverTask.class);
        verify(mapper).insert(captor.capture());
        WalletTurnoverTask task = captor.getValue();
        assertEquals("000000", task.getTenantId());
        assertEquals(1001L, task.getMemberId());
        assertEquals("SC", task.getCurrencyCode());
        assertEquals("ACTIVITY_REWARD", task.getFundPropertyCode());
        assertEquals("PROMOTION", task.getSourceType());
        assertEquals("activity-100", task.getSourceId());
        assertEquals("PR1", task.getBusinessNo());
        assertEquals("WT100", task.getWalletTransactionNo());
        assertEquals(new BigDecimal("20.000000"), task.getRewardAmount());
        assertEquals(new BigDecimal("200.000000"), task.getRequiredTurnover());
        assertEquals(new BigDecimal("0.000000"), task.getCompletedTurnover());
        assertEquals("GAME", task.getGameScopeType());
        assertEquals("slot-001,slot-002", task.getGameScopeValue());
        assertEquals("{\"turnoverMultiplier\":10}", task.getRuleSnapshot());
        assertEquals("PENDING", task.getStatus());
    }

    @Test
    @Tag("local")
    void createFromCreditSkipsWhenRequiredTurnoverIsZero() {
        WalletTurnoverTaskMapper mapper = mock(WalletTurnoverTaskMapper.class);
        WalletTurnoverTaskServiceImpl service = new WalletTurnoverTaskServiceImpl(mapper);

        service.createFromCredit("000000", new WalletCreditBo(), new WalletTransaction(), new BigDecimal("20"), BigDecimal.ZERO, new Date());

        verify(mapper, never()).insert(any(WalletTurnoverTask.class));
    }
}
