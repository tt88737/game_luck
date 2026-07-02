package com.gameluck.wallet.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.constant.SystemConstants;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MapstructUtils;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.wallet.domain.WalletRule;
import com.gameluck.wallet.domain.bo.WalletRuleBo;
import com.gameluck.wallet.domain.vo.WalletRuleVo;
import com.gameluck.wallet.mapper.WalletRuleMapper;
import com.gameluck.wallet.service.IWalletRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Wallet source rule service implementation.
 */
@RequiredArgsConstructor
@Service
public class WalletRuleServiceImpl implements IWalletRuleService {

    private static final String DISABLED = SystemConstants.DISABLE;
    private static final String ENABLED = SystemConstants.NORMAL;

    private final WalletRuleMapper baseMapper;

    @Override
    public TableDataInfo<WalletRuleVo> queryPageList(WalletRuleBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WalletRule> lqw = buildQueryWrapper(bo);
        Page<WalletRuleVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public WalletRuleVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<WalletRuleVo> queryList(WalletRuleBo bo) {
        LambdaQueryWrapper<WalletRule> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public Boolean insertByBo(WalletRuleBo bo) {
        WalletRule add = MapstructUtils.convert(bo, WalletRule.class);
        if (add == null) {
            throw new ServiceException("钱包规则参数错误");
        }
        Date now = new Date();
        add.setId(IdUtil.getSnowflakeNextId());
        add.setCreditEnabled(defaultFlag(add.getCreditEnabled(), DISABLED));
        add.setDebitEnabled(defaultFlag(add.getDebitEnabled(), DISABLED));
        add.setWithdrawEnabled(defaultFlag(add.getWithdrawEnabled(), ENABLED));
        add.setExchangeEnabled(defaultFlag(add.getExchangeEnabled(), ENABLED));
        add.setTurnoverRequired(defaultFlag(add.getTurnoverRequired(), ENABLED));
        add.setDefaultRequiredTurnover(defaultZero(add.getDefaultRequiredTurnover()));
        add.setStatus(defaultFlag(add.getStatus(), ENABLED));
        add.setSortOrder(add.getSortOrder() == null ? 0 : add.getSortOrder());
        add.setVersion(0);
        add.setDelFlag(SystemConstants.NORMAL);
        add.setCreateTime(now);
        add.setUpdateTime(now);
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean updateByBo(WalletRuleBo bo) {
        LambdaUpdateWrapper<WalletRule> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(WalletRule::getId, bo.getId())
            .set(WalletRule::getCurrencyCode, bo.getCurrencyCode())
            .set(WalletRule::getSourceType, bo.getSourceType())
            .set(WalletRule::getRuleName, bo.getRuleName())
            .set(WalletRule::getCreditEnabled, defaultFlag(bo.getCreditEnabled(), DISABLED))
            .set(WalletRule::getDebitEnabled, defaultFlag(bo.getDebitEnabled(), DISABLED))
            .set(WalletRule::getWithdrawEnabled, defaultFlag(bo.getWithdrawEnabled(), ENABLED))
            .set(WalletRule::getExchangeEnabled, defaultFlag(bo.getExchangeEnabled(), ENABLED))
            .set(WalletRule::getReleaseMode, bo.getReleaseMode())
            .set(WalletRule::getTurnoverRequired, defaultFlag(bo.getTurnoverRequired(), ENABLED))
            .set(WalletRule::getDefaultRequiredTurnover, defaultZero(bo.getDefaultRequiredTurnover()))
            .set(WalletRule::getStatus, defaultFlag(bo.getStatus(), ENABLED))
            .set(WalletRule::getSortOrder, bo.getSortOrder() == null ? 0 : bo.getSortOrder())
            .set(WalletRule::getRemark, bo.getRemark())
            .set(WalletRule::getUpdateTime, new Date());
        return baseMapper.update(updateWrapper) > 0;
    }

    @Override
    public WalletRuleVo resolveCreditRule(String tenantId, String currencyCode, String sourceType) {
        WalletRule rule = baseMapper.selectOne(Wrappers.lambdaQuery(WalletRule.class)
            .eq(WalletRule::getTenantId, tenantId)
            .eq(WalletRule::getCurrencyCode, currencyCode)
            .eq(WalletRule::getSourceType, sourceType));
        if (rule == null) {
            throw new ServiceException("钱包规则不存在");
        }
        if (!StringUtils.equals(SystemConstants.NORMAL, rule.getStatus())
            || !StringUtils.equals(SystemConstants.NORMAL, rule.getCreditEnabled())) {
            throw new ServiceException("钱包规则未启用或不允许入账");
        }
        return MapstructUtils.convert(rule, WalletRuleVo.class);
    }

    private LambdaQueryWrapper<WalletRule> buildQueryWrapper(WalletRuleBo bo) {
        LambdaQueryWrapper<WalletRule> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), WalletRule::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getCurrencyCode()), WalletRule::getCurrencyCode, bo.getCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getSourceType()), WalletRule::getSourceType, bo.getSourceType());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), WalletRule::getStatus, bo.getStatus());
        lqw.orderByAsc(WalletRule::getCurrencyCode, WalletRule::getSortOrder, WalletRule::getSourceType);
        return lqw;
    }

    private String defaultFlag(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
