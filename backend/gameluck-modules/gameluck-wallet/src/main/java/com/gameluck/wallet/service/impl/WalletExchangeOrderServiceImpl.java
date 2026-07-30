package com.gameluck.wallet.service.impl;

import cn.hutool.core.util.IdUtil;
import com.gameluck.common.core.constant.SystemConstants;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.wallet.client.domain.bo.ClientExchangeOrderBo;
import com.gameluck.wallet.client.domain.vo.ClientExchangeOrderVo;
import com.gameluck.wallet.domain.WalletExchangeOrder;
import com.gameluck.wallet.domain.WalletExchangeRule;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.domain.bo.WalletDebitBo;
import com.gameluck.wallet.enums.WalletReleaseMode;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.mapper.WalletExchangeOrderMapper;
import com.gameluck.wallet.mapper.WalletExchangeRuleMapper;
import com.gameluck.wallet.service.IWalletCoreService;
import com.gameluck.wallet.service.IWalletExchangeOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * Wallet exchange order runtime service implementation.
 */
@RequiredArgsConstructor
@Service
public class WalletExchangeOrderServiceImpl implements IWalletExchangeOrderService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String ENABLED = "0";
    private static final String SOURCE_TYPE_EXCHANGE = "EXCHANGE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int MONEY_SCALE = 6;

    private final WalletExchangeRuleMapper walletExchangeRuleMapper;
    private final WalletExchangeOrderMapper walletExchangeOrderMapper;
    private final IWalletCoreService walletCoreService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientExchangeOrderVo submit(Long memberId, ClientExchangeOrderBo bo) {
        WalletExchangeRule rule = walletExchangeRuleMapper.selectById(bo.getExchangeRuleId());
        validateRule(rule, bo);
        BigDecimal fromAmount = money(bo.getFromAmount());
        validateDailyLimit(memberId, rule, fromAmount);
        BigDecimal feeAmount = calculateFee(rule, fromAmount);
        BigDecimal toAmount = calculateToAmount(rule, fromAmount, feeAmount);
        Date now = new Date();
        WalletExchangeOrder order = buildPendingOrder(memberId, rule, fromAmount, toAmount, feeAmount, now);
        walletExchangeOrderMapper.insert(order);

        WalletTransaction debit = walletCoreService.debit(buildDebitBo(memberId, order, bo, fromAmount.add(feeAmount)));
        order.setDebitTransactionNo(debit.getTransactionNo());
        if (!WalletTransactionStatus.SUCCESS.name().equals(debit.getStatus())) {
            order.setStatus(STATUS_FAILED);
            order.setFailReason(debit.getFailReason());
            order.setUpdateTime(now);
            walletExchangeOrderMapper.updateById(order);
            return toClientVo(order);
        }

        WalletTransaction credit = walletCoreService.credit(buildCreditBo(memberId, order, bo, rule, toAmount));
        order.setCreditTransactionNo(credit.getTransactionNo());
        order.setStatus(WalletTransactionStatus.SUCCESS.name().equals(credit.getStatus()) ? STATUS_SUCCESS : STATUS_FAILED);
        order.setFailReason(credit.getFailReason());
        order.setUpdateTime(now);
        walletExchangeOrderMapper.updateById(order);
        return toClientVo(order);
    }

    private void validateRule(WalletExchangeRule rule, ClientExchangeOrderBo bo) {
        if (rule == null || !ENABLED.equals(rule.getStatus())) {
            throw new ServiceException(MessageUtils.message("wallet.exchange.rule.not.available"));
        }
        BigDecimal fromAmount = money(bo.getFromAmount());
        if (fromAmount == null || fromAmount.compareTo(ZERO) <= 0) {
            throw new ServiceException(MessageUtils.message("wallet.exchange.amount.positive"));
        }
        BigDecimal min = money(defaultZero(rule.getMinFromAmount()));
        if (min.compareTo(ZERO) > 0 && fromAmount.compareTo(min) < 0) {
            throw new ServiceException(MessageUtils.message("wallet.exchange.amount.below.min"));
        }
        BigDecimal max = money(defaultZero(rule.getMaxFromAmount()));
        if (max.compareTo(ZERO) > 0 && fromAmount.compareTo(max) > 0) {
            throw new ServiceException(MessageUtils.message("wallet.exchange.amount.above.max"));
        }
        if (!"FIXED".equals(rule.getRateType())) {
            throw new ServiceException(MessageUtils.message("wallet.exchange.rate.unsupported"));
        }
    }

    private void validateDailyLimit(Long memberId, WalletExchangeRule rule, BigDecimal fromAmount) {
        BigDecimal dailyLimit = money(defaultZero(rule.getDailyFromLimit()));
        if (dailyLimit.compareTo(ZERO) <= 0) {
            return;
        }
        BigDecimal used = money(defaultZero(walletExchangeOrderMapper.sumSuccessFromAmountToday(
            currentTenantId(), memberId, rule.getId())));
        if (used.add(fromAmount).compareTo(dailyLimit) > 0) {
            throw new ServiceException(MessageUtils.message("wallet.exchange.daily.limit.exceeded"));
        }
    }

    private WalletExchangeOrder buildPendingOrder(Long memberId, WalletExchangeRule rule, BigDecimal fromAmount,
                                                  BigDecimal toAmount, BigDecimal feeAmount, Date now) {
        WalletExchangeOrder order = new WalletExchangeOrder();
        order.setId(IdUtil.getSnowflakeNextId());
        order.setTenantId(currentTenantId());
        order.setExchangeOrderNo("WE" + IdUtil.getSnowflakeNextIdStr());
        order.setMemberId(memberId);
        order.setExchangeRuleId(rule.getId());
        order.setFromCurrencyCode(rule.getFromCurrencyCode());
        order.setFromAmount(fromAmount);
        order.setToCurrencyCode(rule.getToCurrencyCode());
        order.setToAmount(toAmount);
        order.setFeeAmount(feeAmount);
        order.setRuleSnapshot(ruleSnapshot(rule));
        order.setStatus(STATUS_PENDING);
        order.setVersion(0);
        order.setDelFlag(SystemConstants.NORMAL);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        return order;
    }

    private WalletDebitBo buildDebitBo(Long memberId, WalletExchangeOrder order, ClientExchangeOrderBo bo, BigDecimal debitAmount) {
        WalletDebitBo debitBo = new WalletDebitBo();
        debitBo.setIdempotencyKey(idempotencyKey(bo, order, "debit"));
        debitBo.setMemberId(memberId);
        debitBo.setCurrencyCode(order.getFromCurrencyCode());
        debitBo.setSourceType(SOURCE_TYPE_EXCHANGE);
        debitBo.setBusinessNo(order.getExchangeOrderNo());
        debitBo.setAmount(money(debitAmount));
        debitBo.setRemark("wallet exchange debit");
        return debitBo;
    }

    private WalletCreditBo buildCreditBo(Long memberId, WalletExchangeOrder order, ClientExchangeOrderBo bo,
                                         WalletExchangeRule rule, BigDecimal toAmount) {
        WalletCreditBo creditBo = new WalletCreditBo();
        creditBo.setIdempotencyKey(idempotencyKey(bo, order, "credit"));
        creditBo.setMemberId(memberId);
        creditBo.setCurrencyCode(order.getToCurrencyCode());
        creditBo.setSourceType(SOURCE_TYPE_EXCHANGE);
        creditBo.setBusinessNo(order.getExchangeOrderNo());
        creditBo.setAmount(toAmount);
        creditBo.setReleaseMode(WalletReleaseMode.AFTER_TURNOVER.name());
        creditBo.setTurnoverRequiredAmount(resolveRequiredTurnover(rule, toAmount));
        creditBo.setTurnoverMultiplier(defaultZero(rule.getTurnoverMultiplier()));
        creditBo.setGameScopeType(rule.getGameScopeType());
        creditBo.setGameScopeValue(rule.getGameScopeValue());
        creditBo.setRuleSnapshot(order.getRuleSnapshot());
        creditBo.setRemark("wallet exchange credit");
        return creditBo;
    }

    private BigDecimal resolveRequiredTurnover(WalletExchangeRule rule, BigDecimal toAmount) {
        if (!ENABLED.equals(rule.getTurnoverRequired())) {
            return ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return money(toAmount.multiply(defaultZero(rule.getTurnoverMultiplier())));
    }

    private BigDecimal calculateToAmount(WalletExchangeRule rule, BigDecimal fromAmount, BigDecimal feeAmount) {
        return money(fromAmount.multiply(rule.getRateValue()).subtract(feeAmount));
    }

    private BigDecimal calculateFee(WalletExchangeRule rule, BigDecimal fromAmount) {
        String feeType = StringUtils.blankToDefault(rule.getFeeType(), "NONE");
        if ("NONE".equals(feeType)) {
            return ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        if ("FIXED".equals(feeType)) {
            return money(defaultZero(rule.getFeeValue()));
        }
        if ("PERCENT".equals(feeType)) {
            return money(fromAmount.multiply(defaultZero(rule.getFeeValue())).divide(new BigDecimal("100"), MONEY_SCALE, RoundingMode.HALF_UP));
        }
        throw new ServiceException(MessageUtils.message("wallet.exchange.fee.unsupported"));
    }

    private String idempotencyKey(ClientExchangeOrderBo bo, WalletExchangeOrder order, String suffix) {
        String key = StringUtils.blankToDefault(bo.getIdempotencyKey(), order.getExchangeOrderNo());
        return key + ":" + suffix;
    }

    private ClientExchangeOrderVo toClientVo(WalletExchangeOrder order) {
        ClientExchangeOrderVo vo = new ClientExchangeOrderVo();
        vo.setExchangeOrderNo(order.getExchangeOrderNo());
        vo.setExchangeRuleId(order.getExchangeRuleId());
        vo.setFromCurrencyCode(order.getFromCurrencyCode());
        vo.setFromAmount(order.getFromAmount());
        vo.setToCurrencyCode(order.getToCurrencyCode());
        vo.setToAmount(order.getToAmount());
        vo.setFeeAmount(order.getFeeAmount());
        vo.setStatus(order.getStatus());
        vo.setFailReason(order.getFailReason());
        return vo;
    }

    private String ruleSnapshot(WalletExchangeRule rule) {
        return "{"
            + "\"exchangeRuleId\":" + rule.getId()
            + ",\"fromCurrencyCode\":\"" + rule.getFromCurrencyCode() + "\""
            + ",\"toCurrencyCode\":\"" + rule.getToCurrencyCode() + "\""
            + ",\"rateType\":\"" + rule.getRateType() + "\""
            + ",\"rateValue\":\"" + rule.getRateValue() + "\""
            + ",\"feeType\":\"" + rule.getFeeType() + "\""
            + ",\"feeValue\":\"" + rule.getFeeValue() + "\""
            + ",\"turnoverRequired\":\"" + rule.getTurnoverRequired() + "\""
            + ",\"turnoverMultiplier\":\"" + rule.getTurnoverMultiplier() + "\""
            + "}";
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }
}
