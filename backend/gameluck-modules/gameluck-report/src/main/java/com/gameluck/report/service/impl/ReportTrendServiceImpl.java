package com.gameluck.report.service.impl;

import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.report.domain.vo.ReportDailyTrendVo;
import com.gameluck.report.mapper.ReportTrendMapper;
import com.gameluck.report.service.IReportTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class ReportTrendServiceImpl implements IReportTrendService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final int DEFAULT_RANGE = 7;
    private static final int MAX_RANGE = 30;

    private final ReportTrendMapper reportTrendMapper;

    @Override
    public List<ReportDailyTrendVo> dailyTrends(Integer range) {
        int days = normalizeRange(range);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);
        Map<LocalDate, ReportDailyTrendVo> rows = zeroRows(startDate, endDate);
        String tenantId = currentTenantId();
        merge(rows, reportTrendMapper.selectDailyMembers(tenantId, startDate, endDate));
        merge(rows, reportTrendMapper.selectDailyDeposits(tenantId, startDate, endDate));
        merge(rows, reportTrendMapper.selectDailyGames(tenantId, startDate, endDate));
        merge(rows, reportTrendMapper.selectDailyPromotions(tenantId, startDate, endDate));
        merge(rows, reportTrendMapper.selectDailyRedemptions(tenantId, startDate, endDate));
        return rows.values().stream()
            .sorted((left, right) -> right.getReportDate().compareTo(left.getReportDate()))
            .peek(this::normalize)
            .toList();
    }

    private int normalizeRange(Integer range) {
        return range != null && range == MAX_RANGE ? MAX_RANGE : DEFAULT_RANGE;
    }

    private Map<LocalDate, ReportDailyTrendVo> zeroRows(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, ReportDailyTrendVo> rows = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            ReportDailyTrendVo row = new ReportDailyTrendVo();
            row.setReportDate(date);
            normalize(row);
            rows.put(date, row);
        }
        return rows;
    }

    private void merge(Map<LocalDate, ReportDailyTrendVo> target, List<ReportDailyTrendVo> source) {
        for (ReportDailyTrendVo incoming : source) {
            if (incoming == null || incoming.getReportDate() == null || !target.containsKey(incoming.getReportDate())) {
                continue;
            }
            ReportDailyTrendVo row = target.get(incoming.getReportDate());
            if (incoming.getMemberCount() != null) row.setMemberCount(incoming.getMemberCount());
            if (incoming.getDepositOrderCount() != null) row.setDepositOrderCount(incoming.getDepositOrderCount());
            if (incoming.getSuccessfulDepositAmount() != null) row.setSuccessfulDepositAmount(incoming.getSuccessfulDepositAmount());
            if (incoming.getGameOrderCount() != null) row.setGameOrderCount(incoming.getGameOrderCount());
            if (incoming.getTotalBetAmount() != null) row.setTotalBetAmount(incoming.getTotalBetAmount());
            if (incoming.getTotalPayoutAmount() != null) row.setTotalPayoutAmount(incoming.getTotalPayoutAmount());
            if (incoming.getNetGameAmount() != null) row.setNetGameAmount(incoming.getNetGameAmount());
            if (incoming.getPromotionClaimCount() != null) row.setPromotionClaimCount(incoming.getPromotionClaimCount());
            if (incoming.getSuccessfulRewardAmount() != null) row.setSuccessfulRewardAmount(incoming.getSuccessfulRewardAmount());
            if (incoming.getRedemptionOrderCount() != null) row.setRedemptionOrderCount(incoming.getRedemptionOrderCount());
            if (incoming.getPendingRedemptionCount() != null) row.setPendingRedemptionCount(incoming.getPendingRedemptionCount());
            if (incoming.getApprovedRedemptionAmount() != null) row.setApprovedRedemptionAmount(incoming.getApprovedRedemptionAmount());
        }
    }

    private void normalize(ReportDailyTrendVo row) {
        row.setMemberCount(defaultLong(row.getMemberCount()));
        row.setDepositOrderCount(defaultLong(row.getDepositOrderCount()));
        row.setSuccessfulDepositAmount(defaultDecimal(row.getSuccessfulDepositAmount()));
        row.setGameOrderCount(defaultLong(row.getGameOrderCount()));
        row.setTotalBetAmount(defaultDecimal(row.getTotalBetAmount()));
        row.setTotalPayoutAmount(defaultDecimal(row.getTotalPayoutAmount()));
        row.setNetGameAmount(defaultDecimal(row.getNetGameAmount()));
        row.setPromotionClaimCount(defaultLong(row.getPromotionClaimCount()));
        row.setSuccessfulRewardAmount(defaultDecimal(row.getSuccessfulRewardAmount()));
        row.setRedemptionOrderCount(defaultLong(row.getRedemptionOrderCount()));
        row.setPendingRedemptionCount(defaultLong(row.getPendingRedemptionCount()));
        row.setApprovedRedemptionAmount(defaultDecimal(row.getApprovedRedemptionAmount()));
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
