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

/**
 * Wallet turnover task service implementation.
 */
@RequiredArgsConstructor
@Service
public class WalletTurnoverTaskServiceImpl implements IWalletTurnoverTaskService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int MONEY_SCALE = 6;

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
        task.setStatus("PENDING");
        task.setExpireTime(bo.getTurnoverExpireTime());
        task.setRemark(bo.getRemark());
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setVersion(0);
        task.setDelFlag(SystemConstants.NORMAL);
        baseMapper.insert(task);
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value == null ? null : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
