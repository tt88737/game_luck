package com.gameluck.report.service.impl;

import com.gameluck.report.domain.vo.ReportDailyTrendVo;
import com.gameluck.report.mapper.ReportTrendMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportTrendServiceImplTest {

    @Test
    @Tag("local")
    void dailyTrendsDefaultRangeReturnsSevenContinuousRowsInDescendingOrder() {
        ReportTrendMapper mapper = mock(ReportTrendMapper.class);
        ReportTrendServiceImpl service = new ReportTrendServiceImpl(mapper);
        LocalDate today = LocalDate.now();
        ReportDailyTrendVo aggregate = row(today.minusDays(1));
        aggregate.setMemberCount(2L);
        aggregate.setSuccessfulDepositAmount(new BigDecimal("30.000000"));
        when(mapper.selectDailyMembers("000000", today.minusDays(6), today)).thenReturn(List.of(aggregate));
        when(mapper.selectDailyDeposits("000000", today.minusDays(6), today)).thenReturn(List.of(aggregate));
        when(mapper.selectDailyGames("000000", today.minusDays(6), today)).thenReturn(List.of());
        when(mapper.selectDailyPromotions("000000", today.minusDays(6), today)).thenReturn(List.of());
        when(mapper.selectDailyRedemptions("000000", today.minusDays(6), today)).thenReturn(List.of());

        List<ReportDailyTrendVo> result = service.dailyTrends(null);

        assertEquals(7, result.size());
        assertEquals(today, result.get(0).getReportDate());
        assertEquals(today.minusDays(6), result.get(6).getReportDate());
        assertEquals(0L, result.get(0).getMemberCount());
        assertEquals(2L, result.get(1).getMemberCount());
        assertEquals(new BigDecimal("30.000000"), result.get(1).getSuccessfulDepositAmount());
        verify(mapper).selectDailyMembers("000000", today.minusDays(6), today);
    }

    @Test
    @Tag("local")
    void dailyTrendsSupportsThirtyDayRangeAndNormalizesNullValues() {
        ReportTrendMapper mapper = mock(ReportTrendMapper.class);
        ReportTrendServiceImpl service = new ReportTrendServiceImpl(mapper);
        LocalDate today = LocalDate.now();
        ReportDailyTrendVo aggregate = row(today);
        aggregate.setGameOrderCount(null);
        aggregate.setTotalBetAmount(null);
        when(mapper.selectDailyMembers("000000", today.minusDays(29), today)).thenReturn(List.of());
        when(mapper.selectDailyDeposits("000000", today.minusDays(29), today)).thenReturn(List.of());
        when(mapper.selectDailyGames("000000", today.minusDays(29), today)).thenReturn(List.of(aggregate));
        when(mapper.selectDailyPromotions("000000", today.minusDays(29), today)).thenReturn(List.of());
        when(mapper.selectDailyRedemptions("000000", today.minusDays(29), today)).thenReturn(List.of());

        List<ReportDailyTrendVo> result = service.dailyTrends(30);

        assertEquals(30, result.size());
        assertEquals(0L, result.get(0).getGameOrderCount());
        assertEquals(BigDecimal.ZERO, result.get(0).getTotalBetAmount());
    }

    @Test
    @Tag("local")
    void unsupportedRangeFallsBackToSevenDays() {
        ReportTrendMapper mapper = mock(ReportTrendMapper.class);
        ReportTrendServiceImpl service = new ReportTrendServiceImpl(mapper);
        LocalDate today = LocalDate.now();
        when(mapper.selectDailyMembers("000000", today.minusDays(6), today)).thenReturn(List.of());
        when(mapper.selectDailyDeposits("000000", today.minusDays(6), today)).thenReturn(List.of());
        when(mapper.selectDailyGames("000000", today.minusDays(6), today)).thenReturn(List.of());
        when(mapper.selectDailyPromotions("000000", today.minusDays(6), today)).thenReturn(List.of());
        when(mapper.selectDailyRedemptions("000000", today.minusDays(6), today)).thenReturn(List.of());

        List<ReportDailyTrendVo> result = service.dailyTrends(99);

        assertEquals(7, result.size());
        verify(mapper).selectDailyRedemptions("000000", today.minusDays(6), today);
    }

    private ReportDailyTrendVo row(LocalDate reportDate) {
        ReportDailyTrendVo vo = new ReportDailyTrendVo();
        vo.setReportDate(reportDate);
        return vo;
    }
}
