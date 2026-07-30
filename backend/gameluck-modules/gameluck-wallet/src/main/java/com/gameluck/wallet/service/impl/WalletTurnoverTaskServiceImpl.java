package com.gameluck.wallet.service.impl;

import cn.hutool.core.util.IdUtil;
import com.gameluck.common.core.constant.SystemConstants;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.WalletTurnoverTask;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.mapper.WalletTurnoverTaskMapper;
import com.gameluck.wallet.service.IWalletTurnoverTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * Wallet turnover task service implementation.
 */
@RequiredArgsConstructor
@Service
public class WalletTurnoverTaskServiceImpl implements IWalletTurnoverTaskService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int MONEY_SCALE = 6;
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final WalletTurnoverTaskMapper baseMapper;

    @Override
    public void createFromCredit(String tenantId, WalletCreditBo bo, WalletTransaction transaction,
                                 BigDecimal amount, BigDecimal requiredTurnover, Date now) {
        BigDecimal normalizedRequiredTurnover = normalizeAmount(requiredTurnover);
        if (normalizedRequiredTurnover == null || normalizedRequiredTurnover.compareTo(ZERO) <= 0) {
            return;
        }
        WalletTurnoverTask task = new WalletTurnoverTask();
        task.setId(IdUtil.getSnowflakeNextId());
        task.setTenantId(tenantId);
        task.setTurnoverTaskNo("WTT" + IdUtil.getSnowflakeNextIdStr());
        task.setMemberId(bo.getMemberId());
        task.setCurrencyCode(bo.getCurrencyCode());
        task.setFundPropertyCode(StringUtils.blankToDefault(bo.getFundPropertyCode(), bo.getSourceType()));
        task.setSourceType(bo.getSourceType());
        task.setSourceId(bo.getSourceId());
        task.setBusinessNo(bo.getBusinessNo());
        task.setWalletTransactionNo(transaction.getTransactionNo());
        task.setRewardAmount(normalizeAmount(amount));
        task.setRequiredTurnover(normalizedRequiredTurnover);
        task.setCompletedTurnover(normalizeAmount(ZERO));
        task.setGameScopeType(StringUtils.blankToDefault(bo.getGameScopeType(), "ALL"));
        task.setGameScopeValue(bo.getGameScopeValue());
        task.setRuleSnapshot(bo.getRuleSnapshot());
        task.setStatus(STATUS_PENDING);
        task.setExpireTime(bo.getTurnoverExpireTime());
        task.setRemark(bo.getRemark());
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setVersion(0);
        task.setDelFlag(SystemConstants.NORMAL);
        baseMapper.insert(task);
    }

    @Override
    public int applyValidTurnover(String tenantId, Long memberId, String currencyCode, BigDecimal validTurnoverAmount, Date now) {
        BigDecimal remainTurnover = normalizeAmount(validTurnoverAmount);
        if (remainTurnover == null || remainTurnover.compareTo(ZERO) <= 0) {
            return 0;
        }

        List<WalletTurnoverTask> tasks = baseMapper.selectPendingByMemberForUpdate(tenantId, memberId, currencyCode, STATUS_PENDING);
        int completedCount = 0;
        for (WalletTurnoverTask task : tasks) {
            BigDecimal completed = normalizeAmount(defaultZero(task.getCompletedTurnover()));
            BigDecimal required = normalizeAmount(defaultZero(task.getRequiredTurnover()));
            BigDecimal need = required.subtract(completed).max(ZERO);
            if (remainTurnover.compareTo(ZERO) <= 0 && need.compareTo(ZERO) > 0) {
                continue;
            }

            BigDecimal applied = remainTurnover.min(need);
            BigDecimal completedAfter = normalizeAmount(completed.add(applied));
            remainTurnover = normalizeAmount(remainTurnover.subtract(applied));

            task.setCompletedTurnover(completedAfter);
            if (completedAfter.compareTo(required) >= 0) {
                task.setStatus(STATUS_COMPLETED);
                task.setCompleteTime(now);
                completedCount++;
            }
            task.setUpdateTime(now);
            baseMapper.updateById(task);
        }
        return completedCount;
    }

    @Override
    public int cancelPendingByPurchase(String tenantId, Long memberId, String purchaseOrderNo,
                                       String reversalNo, Date now) {
        return baseMapper.cancelPendingByPurchase(tenantId, memberId, purchaseOrderNo,
            STATUS_PENDING, STATUS_CANCELLED, "Purchase reversal " + reversalNo, now);
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value == null ? null : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }
}
