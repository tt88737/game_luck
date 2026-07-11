package com.gameluck.wallet.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.constant.SystemConstants;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MapstructUtils;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.wallet.domain.WalletRule;
import com.gameluck.wallet.domain.bo.WalletRuleBo;
import com.gameluck.wallet.domain.vo.WalletRuleTemplateVo;
import com.gameluck.wallet.domain.vo.WalletRuleVo;
import com.gameluck.wallet.mapper.WalletRuleMapper;
import com.gameluck.wallet.service.IWalletRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Wallet source rule service implementation.
 */
@RequiredArgsConstructor
@Service
public class WalletRuleServiceImpl implements IWalletRuleService {

    private static final String DISABLED = SystemConstants.DISABLE;
    private static final String ENABLED = SystemConstants.NORMAL;
    private static final String DEFAULT_TENANT_ID = "000000";

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
            throw new ServiceException(MessageUtils.message("wallet.rule.param.error"));
        }
        Date now = new Date();
        add.setId(IdUtil.getSnowflakeNextId());
        add.setSourceType(canonicalSourceType(add.getSourceType()));
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
            .set(WalletRule::getSourceType, canonicalSourceType(bo.getSourceType()))
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
        List<WalletRule> rules = baseMapper.selectList(Wrappers.lambdaQuery(WalletRule.class)
            .eq(WalletRule::getTenantId, tenantId)
            .eq(WalletRule::getCurrencyCode, currencyCode)
            .in(WalletRule::getSourceType, sourceTypeVariants(sourceType)));
        WalletRule rule = preferredRule(filteredRules(rules, tenantId, currencyCode), sourceType);
        if (rule == null) {
            throw new ServiceException(MessageUtils.message("wallet.rule.not.exists"));
        }
        if (!StringUtils.equals(SystemConstants.NORMAL, rule.getStatus())
            || !StringUtils.equals(SystemConstants.NORMAL, rule.getCreditEnabled())) {
            throw new ServiceException(MessageUtils.message("wallet.rule.credit.disabled"));
        }
        return toVo(rule);
    }

    @Override
    public List<WalletRuleTemplateVo> listDefaultTemplates() {
        List<WalletRuleTemplateVo> templates = new ArrayList<>();
        templates.add(template("GC", "REGISTER_BONUS", "Registration bonus", "GC registration bonus", ENABLED, DISABLED, DISABLED, DISABLED, "IMMEDIATE", DISABLED, 10));
        templates.add(template("SC", "REGISTER_BONUS", "Registration bonus", "SC registration bonus", ENABLED, DISABLED, DISABLED, DISABLED, "IMMEDIATE", DISABLED, 20));
        templates.add(template("GC", "DAILY_REWARD", "Daily login reward", "GC daily login reward", ENABLED, DISABLED, DISABLED, DISABLED, "IMMEDIATE", DISABLED, 30));
        templates.add(template("SC", "DAILY_REWARD", "Daily login reward", "SC daily login reward", ENABLED, DISABLED, ENABLED, DISABLED, "IMMEDIATE", DISABLED, 40));
        templates.add(template("GC", "PROMOTION", "Promotion", "GC promotion", ENABLED, DISABLED, DISABLED, DISABLED, "IMMEDIATE", DISABLED, 50));
        templates.add(template("SC", "PROMOTION", "Promotion", "SC promotion", ENABLED, DISABLED, DISABLED, DISABLED, "AFTER_TURNOVER", ENABLED, 60));
        templates.add(template("RC", "DEPOSIT", "Deposit", "RC deposit", ENABLED, ENABLED, ENABLED, DISABLED, "IMMEDIATE", DISABLED, 70));
        templates.add(template("GC", "GAME_PROFIT", "Game profit", "GC game profit", ENABLED, ENABLED, DISABLED, DISABLED, "NEVER", DISABLED, 80));
        templates.add(template("SC", "GAME_PROFIT", "Game profit", "SC game profit", ENABLED, ENABLED, DISABLED, ENABLED, "AFTER_TURNOVER", ENABLED, 90));
        templates.add(template("SC", "GAME_REFUND", "Game refund", "SC game refund", ENABLED, ENABLED, DISABLED, ENABLED, "IMMEDIATE", ENABLED, 100));
        templates.add(template("GC", "MANUAL_ADJUST", "Manual adjustment", "GC manual adjustment", ENABLED, ENABLED, DISABLED, DISABLED, "IMMEDIATE", DISABLED, 110));
        templates.add(template("SC", "MANUAL_ADJUST", "Manual adjustment", "SC manual adjustment", ENABLED, ENABLED, DISABLED, ENABLED, "MANUAL_REVIEW", DISABLED, 120));
        templates.add(template("RC", "MANUAL_ADJUST", "Manual adjustment", "RC manual adjustment", ENABLED, ENABLED, ENABLED, DISABLED, "MANUAL_REVIEW", DISABLED, 130));
        return templates;
    }

    @Override
    public List<WalletRuleTemplateVo> previewMissingDefaultRules(String tenantId) {
        String ruleTenantId = defaultTenantId(tenantId);
        Set<String> existingKeys = existingTemplateKeys(ruleTenantId);
        return listDefaultTemplates().stream()
            .peek(template -> {
                boolean exists = existingKeys.contains(templateKey(template.getCurrencyCode(), template.getSourceType()));
                template.setExists(exists);
                template.setWillCreate(!exists);
            })
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int seedMissingDefaultRules(String tenantId) {
        String ruleTenantId = defaultTenantId(tenantId);
        Date now = new Date();
        int inserted = 0;
        for (WalletRuleTemplateVo template : previewMissingDefaultRules(ruleTenantId)) {
            if (Boolean.TRUE.equals(template.getWillCreate())
                && !existsRule(ruleTenantId, template.getCurrencyCode(), template.getSourceType())) {
                try {
                    inserted += baseMapper.insert(buildRule(ruleTenantId, template, now)) > 0 ? 1 : 0;
                } catch (DuplicateKeyException ignored) {
                    // Concurrent seed won the race; keep the operation idempotent.
                }
            }
        }
        return inserted;
    }

    @Override
    public String canonicalSourceType(String sourceType) {
        if (StringUtils.isBlank(sourceType)) {
            return sourceType;
        }
        String normalized = sourceType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "GAME_PAYOUT" -> "GAME_PROFIT";
            case "ADJUST", "ADJUSTMENT" -> "MANUAL_ADJUST";
            default -> normalized;
        };
    }

    private LambdaQueryWrapper<WalletRule> buildQueryWrapper(WalletRuleBo bo) {
        LambdaQueryWrapper<WalletRule> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), WalletRule::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getCurrencyCode()), WalletRule::getCurrencyCode, bo.getCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getSourceType()), WalletRule::getSourceType, canonicalSourceType(bo.getSourceType()));
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), WalletRule::getStatus, bo.getStatus());
        lqw.orderByAsc(WalletRule::getCurrencyCode, WalletRule::getSortOrder, WalletRule::getSourceType);
        return lqw;
    }

    private WalletRuleTemplateVo template(String currencyCode, String sourceType, String sourceLabel, String ruleName,
                                          String creditEnabled, String debitEnabled, String withdrawEnabled,
                                          String exchangeEnabled, String releaseMode, String turnoverRequired,
                                          int sortOrder) {
        WalletRuleTemplateVo template = new WalletRuleTemplateVo();
        template.setCurrencyCode(currencyCode);
        template.setSourceType(sourceType);
        template.setSourceLabel(sourceLabel);
        template.setRuleName(ruleName);
        template.setCreditEnabled(creditEnabled);
        template.setDebitEnabled(debitEnabled);
        template.setWithdrawEnabled(withdrawEnabled);
        template.setExchangeEnabled(exchangeEnabled);
        template.setReleaseMode(releaseMode);
        template.setTurnoverRequired(turnoverRequired);
        template.setDefaultRequiredTurnover(BigDecimal.ZERO);
        template.setStatus(ENABLED);
        template.setSortOrder(sortOrder);
        template.setRemark("Default wallet rule template");
        template.setExists(false);
        template.setWillCreate(false);
        return template;
    }

    private Set<String> existingTemplateKeys(String tenantId) {
        List<WalletRule> existingRules = baseMapper.selectList(Wrappers.lambdaQuery(WalletRule.class)
            .eq(WalletRule::getTenantId, tenantId));
        Set<String> keys = new HashSet<>();
        for (WalletRule rule : existingRules) {
            keys.add(templateKey(rule.getCurrencyCode(), rule.getSourceType()));
        }
        return keys;
    }

    private boolean existsRule(String tenantId, String currencyCode, String sourceType) {
        List<WalletRule> existingRules = baseMapper.selectList(Wrappers.lambdaQuery(WalletRule.class)
            .eq(WalletRule::getTenantId, tenantId)
            .eq(WalletRule::getCurrencyCode, currencyCode)
            .in(WalletRule::getSourceType, sourceTypeVariants(sourceType)));
        return preferredRule(filteredRules(existingRules, tenantId, currencyCode), sourceType) != null;
    }

    private WalletRule buildRule(String tenantId, WalletRuleTemplateVo template, Date now) {
        WalletRule rule = new WalletRule();
        rule.setId(IdUtil.getSnowflakeNextId());
        rule.setTenantId(tenantId);
        rule.setCurrencyCode(template.getCurrencyCode());
        rule.setSourceType(canonicalSourceType(template.getSourceType()));
        rule.setRuleName(template.getRuleName());
        rule.setCreditEnabled(template.getCreditEnabled());
        rule.setDebitEnabled(template.getDebitEnabled());
        rule.setWithdrawEnabled(template.getWithdrawEnabled());
        rule.setExchangeEnabled(template.getExchangeEnabled());
        rule.setReleaseMode(template.getReleaseMode());
        rule.setTurnoverRequired(template.getTurnoverRequired());
        rule.setDefaultRequiredTurnover(defaultZero(template.getDefaultRequiredTurnover()));
        rule.setStatus(template.getStatus());
        rule.setSortOrder(template.getSortOrder());
        rule.setRemark(template.getRemark());
        rule.setVersion(0);
        rule.setDelFlag(SystemConstants.NORMAL);
        rule.setCreateTime(now);
        rule.setUpdateTime(now);
        return rule;
    }

    private String defaultTenantId(String tenantId) {
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }

    private String templateKey(String currencyCode, String sourceType) {
        return currencyCode + ":" + canonicalSourceType(sourceType);
    }

    private Set<String> sourceTypeVariants(String sourceType) {
        String canonical = canonicalSourceType(sourceType);
        Set<String> variants = new HashSet<>();
        variants.add(canonical);
        if (StringUtils.equals("GAME_PROFIT", canonical)) {
            variants.add("GAME_PAYOUT");
        }
        if (StringUtils.equals("MANUAL_ADJUST", canonical)) {
            variants.add("ADJUST");
            variants.add("ADJUSTMENT");
        }
        return variants;
    }

    private WalletRule preferredRule(List<WalletRule> rules, String sourceType) {
        String canonical = canonicalSourceType(sourceType);
        WalletRule aliasRule = null;
        for (WalletRule rule : rules) {
            if (!StringUtils.equals(canonical, canonicalSourceType(rule.getSourceType()))) {
                continue;
            }
            if (StringUtils.equals(canonical, rule.getSourceType())) {
                return rule;
            }
            if (aliasRule == null) {
                aliasRule = rule;
            }
        }
        return aliasRule;
    }

    private List<WalletRule> filteredRules(List<WalletRule> rules, String tenantId, String currencyCode) {
        return rules.stream()
            .filter(rule -> StringUtils.equals(tenantId, rule.getTenantId())
                && StringUtils.equals(currencyCode, rule.getCurrencyCode()))
            .toList();
    }

    private WalletRuleVo toVo(WalletRule rule) {
        WalletRuleVo vo = new WalletRuleVo();
        vo.setId(rule.getId());
        vo.setTenantId(rule.getTenantId());
        vo.setCurrencyCode(rule.getCurrencyCode());
        vo.setSourceType(rule.getSourceType());
        vo.setRuleName(rule.getRuleName());
        vo.setCreditEnabled(rule.getCreditEnabled());
        vo.setDebitEnabled(rule.getDebitEnabled());
        vo.setWithdrawEnabled(rule.getWithdrawEnabled());
        vo.setExchangeEnabled(rule.getExchangeEnabled());
        vo.setReleaseMode(rule.getReleaseMode());
        vo.setTurnoverRequired(rule.getTurnoverRequired());
        vo.setDefaultRequiredTurnover(rule.getDefaultRequiredTurnover());
        vo.setStatus(rule.getStatus());
        vo.setSortOrder(rule.getSortOrder());
        vo.setRemark(rule.getRemark());
        vo.setCreateTime(rule.getCreateTime());
        vo.setUpdateTime(rule.getUpdateTime());
        vo.setVersion(rule.getVersion());
        return vo;
    }

    private String defaultFlag(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
