package com.gameluck.report.service.impl;

import com.gameluck.report.domain.vo.ReportOverviewSummaryVo;
import com.gameluck.report.mapper.ReportOverviewMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportOverviewServiceImplTest {

    @Test
    @Tag("local")
    void querySummaryUsesDefaultTenantAndNormalizesNullMetrics() {
        ReportOverviewMapper mapper = mock(ReportOverviewMapper.class);
        ReportOverviewServiceImpl service = new ReportOverviewServiceImpl(mapper);

        ReportOverviewSummaryVo mapperResult = new ReportOverviewSummaryVo();
        mapperResult.setMemberCount(3L);
        mapperResult.setWalletAccountCount(7L);
        mapperResult.setWalletAvailableAmount(new BigDecimal("128.500000"));
        mapperResult.setWalletFrozenAmount(null);
        mapperResult.setDepositOrderCount(2L);
        mapperResult.setSuccessfulDepositAmount(new BigDecimal("40.000000"));
        mapperResult.setGameOrderCount(5L);
        mapperResult.setTotalBetAmount(new BigDecimal("15.000000"));
        mapperResult.setTotalPayoutAmount(new BigDecimal("21.000000"));
        mapperResult.setNetGameAmount(new BigDecimal("6.000000"));
        mapperResult.setPromotionClaimCount(4L);
        mapperResult.setSuccessfulRewardAmount(new BigDecimal("12.000000"));
        mapperResult.setRedemptionOrderCount(3L);
        mapperResult.setPendingRedemptionCount(1L);
        mapperResult.setApprovedRedemptionCount(1L);
        mapperResult.setRejectedRedemptionCount(1L);
        mapperResult.setApprovedRedemptionAmount(new BigDecimal("8.000000"));
        when(mapper.selectSummary("000000")).thenReturn(mapperResult);

        ReportOverviewSummaryVo result = service.querySummary();

        assertEquals(3L, result.getMemberCount());
        assertEquals(7L, result.getWalletAccountCount());
        assertEquals(new BigDecimal("128.500000"), result.getWalletAvailableAmount());
        assertEquals(BigDecimal.ZERO, result.getWalletFrozenAmount());
        assertEquals(new BigDecimal("6.000000"), result.getNetGameAmount());
        assertEquals(1L, result.getPendingRedemptionCount());
        assertEquals(new BigDecimal("8.000000"), result.getApprovedRedemptionAmount());
        verify(mapper).selectSummary("000000");
    }
}

