package com.gameluck.report.service;

import com.gameluck.report.domain.vo.ReportDailyTrendVo;

import java.util.List;

public interface IReportTrendService {
    List<ReportDailyTrendVo> dailyTrends(Integer range);
}
