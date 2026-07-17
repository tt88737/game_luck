package com.gameluck.wallet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.constant.SystemConstants;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.wallet.client.domain.vo.ClientExchangeOptionVo;
import com.gameluck.wallet.domain.WalletExchangeRule;
import com.gameluck.wallet.domain.bo.WalletExchangeRuleBo;
import com.gameluck.wallet.domain.vo.WalletExchangeRuleVo;
import com.gameluck.wallet.mapper.WalletExchangeRuleMapper;
import com.gameluck.wallet.service.IWalletExchangeRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * Wallet exchange rule service implementation.
 */
@RequiredArgsConstructor
@Service
public class WalletExchangeRuleServiceImpl implements IWalletExchangeRuleService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String ENABLED = "0";
    private static final String DISABLED = "1";
    private static final String DEFAULT_RATE_TYPE = "FIXED";
    private static final String DEFAULT_FEE_TYPE = "NONE";
    private static final String DEFAULT_TURNOVER_REQUIRED = "1";
    private static final String GAME_SCOPE_ALL = "ALL";
    private static final int MONEY_SCALE = 6;
    private static final int RATE_SCALE = 8;

    private final WalletExchangeRuleMapper baseMapper;

    @Override
    public TableDataInfo<WalletExchangeRuleVo> queryPageList(WalletExchangeRuleBo bo, PageQuery pageQuery) {
        Page<WalletExchangeRuleVo> page = baseMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return TableDataInfo.build(page);
    }

    @Override
    public WalletExchangeRuleVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<WalletExchangeRuleVo> queryList(WalletExchangeRuleBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    public int insertByBo(WalletExchangeRuleBo bo) {
        validateRule(bo);
        WalletExchangeRule add = BeanUtil.toBean(bo, WalletExchangeRule.class);
        add.setId(IdUtil.getSnowflakeNextId());
        add.setTenantId(StringUtils.blankToDefault(bo.getTenantId(), currentTenantId()));
        normalizeDefaults(add);
        add.setStatus(StringUtils.blankToDefault(bo.getStatus(), DISABLED));
        add.setVersion(0);
        add.setDelFlag(SystemConstants.NORMAL);
        Date now = new Date();
        add.setCreateTime(now);
        add.setUpdateTime(now);
        return baseMapper.insert(add);
    }

    @Override
    public int updateByBo(WalletExchangeRuleBo bo) {
        validateRule(bo);
        WalletExchangeRule update = BeanUtil.toBean(bo, WalletExchangeRule.class);
        normalizeDefaults(update);
        update.setUpdateTime(new Date());
        return baseMapper.updateById(update);
    }

    @Override
    public List<ClientExchangeOptionVo> listOptions(Long memberId, String channel) {
        Date now = new Date();
        LambdaQueryWrapper<WalletExchangeRule> lqw = Wrappers.lambdaQuery();
        lqw.eq(WalletExchangeRule::getTenantId, currentTenantId())
            .eq(WalletExchangeRule::getStatus, ENABLED)
            .and(wrapper -> wrapper.isNull(WalletExchangeRule::getStartTime)
                .or()
                .le(WalletExchangeRule::getStartTime, now))
            .and(wrapper -> wrapper.isNull(WalletExchangeRule::getEndTime)
                .or()
                .ge(WalletExchangeRule::getEndTime, now))
            .orderByAsc(WalletExchangeRule::getFromCurrencyCode, WalletExchangeRule::getToCurrencyCode);
        if (StringUtils.isNotBlank(channel)) {
            lqw.and(wrapper -> wrapper.isNull(WalletExchangeRule::getChannel)
                .or()
                .eq(WalletExchangeRule::getChannel, channel));
        } else {
            lqw.isNull(WalletExchangeRule::getChannel);
        }
        return baseMapper.selectList(lqw).stream().map(this::toClientOption).toList();
    }

    private LambdaQueryWrapper<WalletExchangeRule> buildQueryWrapper(WalletExchangeRuleBo bo) {
        LambdaQueryWrapper<WalletExchangeRule> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), WalletExchangeRule::getTenantId, bo.getTenantId());
        lqw.like(StringUtils.isNotBlank(bo.getRuleName()), WalletExchangeRule::getRuleName, bo.getRuleName());
        lqw.eq(StringUtils.isNotBlank(bo.getFromCurrencyCode()), WalletExchangeRule::getFromCurrencyCode, bo.getFromCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getToCurrencyCode()), WalletExchangeRule::getToCurrencyCode, bo.getToCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getRateType()), WalletExchangeRule::getRateType, bo.getRateType());
        lqw.eq(StringUtils.isNotBlank(bo.getFeeType()), WalletExchangeRule::getFeeType, bo.getFeeType());
        lqw.eq(StringUtils.isNotBlank(bo.getCountryCode()), WalletExchangeRule::getCountryCode, bo.getCountryCode());
        lqw.eq(StringUtils.isNotBlank(bo.getStateCode()), WalletExchangeRule::getStateCode, bo.getStateCode());
        lqw.eq(StringUtils.isNotBlank(bo.getMemberTag()), WalletExchangeRule::getMemberTag, bo.getMemberTag());
        lqw.eq(StringUtils.isNotBlank(bo.getChannel()), WalletExchangeRule::getChannel, bo.getChannel());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), WalletExchangeRule::getStatus, bo.getStatus());
        lqw.orderByDesc(WalletExchangeRule::getCreateTime);
        return lqw;
    }

    private void validateRule(WalletExchangeRuleBo bo) {
        if (equalsIgnoreCase(bo.getFromCurrencyCode(), bo.getToCurrencyCode())) {
            throw new ServiceException(MessageUtils.message("wallet.exchange.rule.currency.same"));
        }
        if (bo.getRateValue() == null || bo.getRateValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(MessageUtils.message("wallet.exchange.rule.rate.positive"));
        }
        if (equalsIgnoreCase("PERCENT", bo.getFeeType())) {
            BigDecimal feeValue = bo.getFeeValue();
            if (feeValue == null || feeValue.compareTo(BigDecimal.ZERO) < 0 || feeValue.compareTo(new BigDecimal("100")) > 0) {
                throw new ServiceException(MessageUtils.message("wallet.exchange.rule.fee.percent.invalid"));
            }
        }
        if (bo.getTurnoverMultiplier() != null && bo.getTurnoverMultiplier().compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException(MessageUtils.message("wallet.exchange.rule.turnover.multiplier.invalid"));
        }
    }

    private void normalizeDefaults(WalletExchangeRule rule) {
        rule.setRateType(StringUtils.blankToDefault(rule.getRateType(), DEFAULT_RATE_TYPE));
        rule.setRateValue(scale(rule.getRateValue(), RATE_SCALE));
        rule.setMinFromAmount(scale(defaultZero(rule.getMinFromAmount()), MONEY_SCALE));
        rule.setMaxFromAmount(scale(defaultZero(rule.getMaxFromAmount()), MONEY_SCALE));
        rule.setDailyFromLimit(scale(defaultZero(rule.getDailyFromLimit()), MONEY_SCALE));
        rule.setFeeType(StringUtils.blankToDefault(rule.getFeeType(), DEFAULT_FEE_TYPE));
        rule.setFeeValue(scale(defaultZero(rule.getFeeValue()), MONEY_SCALE));
        rule.setTurnoverRequired(StringUtils.blankToDefault(rule.getTurnoverRequired(), DEFAULT_TURNOVER_REQUIRED));
        rule.setTurnoverMultiplier(scale(defaultZero(rule.getTurnoverMultiplier()), 4));
        rule.setGameScopeType(StringUtils.blankToDefault(rule.getGameScopeType(), GAME_SCOPE_ALL));
    }

    private ClientExchangeOptionVo toClientOption(WalletExchangeRule rule) {
        ClientExchangeOptionVo vo = new ClientExchangeOptionVo();
        vo.setExchangeRuleId(rule.getId());
        vo.setRuleName(rule.getRuleName());
        vo.setFromCurrencyCode(rule.getFromCurrencyCode());
        vo.setToCurrencyCode(rule.getToCurrencyCode());
        vo.setRateType(rule.getRateType());
        vo.setRateValue(rule.getRateValue());
        vo.setMinFromAmount(rule.getMinFromAmount());
        vo.setMaxFromAmount(rule.getMaxFromAmount());
        vo.setDailyFromLimit(rule.getDailyFromLimit());
        vo.setFeeType(rule.getFeeType());
        vo.setFeeValue(rule.getFeeValue());
        vo.setTurnoverRequired(rule.getTurnoverRequired());
        vo.setTurnoverMultiplier(rule.getTurnoverMultiplier());
        return vo;
    }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }
}
