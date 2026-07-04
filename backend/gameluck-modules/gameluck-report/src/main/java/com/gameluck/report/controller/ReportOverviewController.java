package com.gameluck.report.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.report.domain.vo.ReportOverviewSummaryVo;
import com.gameluck.report.service.IReportOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Report overview controller.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/report/overview")
public class ReportOverviewController extends BaseController {

    private final IReportOverviewService reportOverviewService;

    @SaCheckPermission("report:overview:query")
    @GetMapping("/summary")
    public R<ReportOverviewSummaryVo> summary() {
        return R.ok(reportOverviewService.querySummary());
    }
}

