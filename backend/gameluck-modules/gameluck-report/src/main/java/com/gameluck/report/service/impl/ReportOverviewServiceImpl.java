package com.gameluck.report.service.impl;

import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.report.domain.vo.ReportOverviewSummaryVo;
import com.gameluck.report.mapper.ReportOverviewMapper;
import com.gameluck.report.service.IReportOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Report overview service implementation.
 */
@RequiredArgsConstructor
@Service
public class ReportOverviewServiceImpl implements IReportOverviewService {

    private static final String DEFAULT_TENANT_ID = "000000";

    private final ReportOverviewMapper reportOverviewMapper;

    @Override
    public ReportOverviewSummaryVo querySummary() {
        ReportOverviewSummaryVo summary = reportOverviewMapper.selectSummary(currentTenantId());
        if (summary == null) {
            summary = new ReportOverviewSummaryVo();
        }
        normalize(summary);
        return summary;
    }

    private void normalize(ReportOverviewSummaryVo summary) {
        summary.setMemberCount(defaultLong(summary.getMemberCount()));
        summary.setWalletAccountCount(defaultLong(summary.getWalletAccountCount()));
        summary.setWalletAvailableAmount(defaultDecimal(summary.getWalletAvailableAmount()));
        summary.setWalletFrozenAmount(defaultDecimal(summary.getWalletFrozenAmount()));
        summary.setDepositOrderCount(defaultLong(summary.getDepositOrderCount()));
        summary.setSuccessfulDepositAmount(defaultDecimal(summary.getSuccessfulDepositAmount()));
        summary.setGameOrderCount(defaultLong(summary.getGameOrderCount()));
        summary.setTotalBetAmount(defaultDecimal(summary.getTotalBetAmount()));
        summary.setTotalPayoutAmount(defaultDecimal(summary.getTotalPayoutAmount()));
        summary.setNetGameAmount(defaultDecimal(summary.getNetGameAmount()));
        summary.setPromotionClaimCount(defaultLong(summary.getPromotionClaimCount()));
        summary.setSuccessfulRewardAmount(defaultDecimal(summary.getSuccessfulRewardAmount()));
        summary.setRedemptionOrderCount(defaultLong(summary.getRedemptionOrderCount()));
        summary.setPendingRedemptionCount(defaultLong(summary.getPendingRedemptionCount()));
        summary.setApprovedRedemptionCount(defaultLong(summary.getApprovedRedemptionCount()));
        summary.setRejectedRedemptionCount(defaultLong(summary.getRejectedRedemptionCount()));
        summary.setApprovedRedemptionAmount(defaultDecimal(summary.getApprovedRedemptionAmount()));
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }
}

