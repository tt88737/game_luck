package com.gameluck.report.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.report.domain.bo.ReportDailyTrendQueryBo;
import com.gameluck.report.domain.vo.ReportDailyTrendVo;
import com.gameluck.report.service.IReportTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/report/trends")
public class ReportTrendController extends BaseController {

    private final IReportTrendService reportTrendService;

    @SaCheckPermission("report:trends:query")
    @GetMapping("/daily")
    public R<List<ReportDailyTrendVo>> daily(@ModelAttribute ReportDailyTrendQueryBo bo) {
        return R.ok(reportTrendService.dailyTrends(bo.getRange()));
    }
}
