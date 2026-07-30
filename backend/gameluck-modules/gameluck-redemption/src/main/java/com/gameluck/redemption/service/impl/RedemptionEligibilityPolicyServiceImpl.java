package com.gameluck.redemption.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.constant.SystemConstants;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.redemption.domain.RedemptionEligibilityPolicy;
import com.gameluck.redemption.domain.bo.RedemptionEligibilityPolicyBo;
import com.gameluck.redemption.domain.vo.RedemptionEligibilityPolicyVo;
import com.gameluck.redemption.mapper.RedemptionEligibilityPolicyMapper;
import com.gameluck.redemption.service.IRedemptionEligibilityPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
@Service
public class RedemptionEligibilityPolicyServiceImpl implements IRedemptionEligibilityPolicyService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String EFFECT_DENY = "DENY";
    private static final String EFFECT_ALLOW = "ALLOW";

    private final RedemptionEligibilityPolicyMapper policyMapper;

    @Override
    public TableDataInfo<RedemptionEligibilityPolicyVo> queryPageList(RedemptionEligibilityPolicyBo bo, PageQuery pageQuery) {
        Page<RedemptionEligibilityPolicyVo> page = policyMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return TableDataInfo.build(page);
    }

    @Override
    public RedemptionEligibilityPolicyVo queryById(Long id) {
        return policyMapper.selectVoById(id);
    }

    @Override
    public int insertByBo(RedemptionEligibilityPolicyBo bo) {
        RedemptionEligibilityPolicy add = BeanUtil.toBean(bo, RedemptionEligibilityPolicy.class);
        add.setId(IdUtil.getSnowflakeNextId());
        add.setTenantId(StringUtils.blankToDefault(bo.getTenantId(), currentTenantId()));
        normalizeDefaults(add);
        add.setVersion(0);
        add.setDelFlag(SystemConstants.NORMAL);
        add.setCreateTime(new Date());
        add.setUpdateTime(add.getCreateTime());
        return policyMapper.insert(add);
    }

    @Override
    public int updateByBo(RedemptionEligibilityPolicyBo bo) {
        RedemptionEligibilityPolicy update = BeanUtil.toBean(bo, RedemptionEligibilityPolicy.class);
        normalizeDefaults(update);
        update.setUpdateTime(new Date());
        return policyMapper.updateById(update);
    }

    @Override
    public boolean isEligible(String tenantId, String currencyCode, String countryCode, String stateCode, String channel) {
        Date now = new Date();
        List<RedemptionEligibilityPolicy> matches = policyMapper.selectList(Wrappers.lambdaQuery(RedemptionEligibilityPolicy.class)
                .eq(RedemptionEligibilityPolicy::getTenantId, StringUtils.blankToDefault(tenantId, DEFAULT_TENANT_ID))
                .eq(RedemptionEligibilityPolicy::getCurrencyCode, normalize(currencyCode))
                .eq(RedemptionEligibilityPolicy::getStatus, SystemConstants.NORMAL))
            .stream()
            .filter(policy -> SystemConstants.NORMAL.equals(policy.getStatus()))
            .filter(policy -> activeAt(policy, now))
            .filter(policy -> blankOrEquals(policy.getCountryCode(), countryCode))
            .filter(policy -> blankOrEquals(policy.getStateCode(), stateCode))
            .filter(policy -> blankOrEquals(policy.getChannel(), channel))
            .sorted(Comparator
                .comparing(RedemptionEligibilityPolicy::getPriority, Comparator.nullsFirst(Integer::compareTo)).reversed()
                .thenComparing(policy -> EFFECT_DENY.equalsIgnoreCase(policy.getEffect()) ? 0 : 1))
            .toList();
        if (matches.isEmpty()) {
            return true;
        }
        return EFFECT_ALLOW.equalsIgnoreCase(matches.get(0).getEffect());
    }

    private LambdaQueryWrapper<RedemptionEligibilityPolicy> buildQueryWrapper(RedemptionEligibilityPolicyBo bo) {
        LambdaQueryWrapper<RedemptionEligibilityPolicy> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), RedemptionEligibilityPolicy::getTenantId, bo.getTenantId());
        lqw.like(StringUtils.isNotBlank(bo.getPolicyName()), RedemptionEligibilityPolicy::getPolicyName, bo.getPolicyName());
        lqw.eq(StringUtils.isNotBlank(bo.getCurrencyCode()), RedemptionEligibilityPolicy::getCurrencyCode, bo.getCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getCountryCode()), RedemptionEligibilityPolicy::getCountryCode, bo.getCountryCode());
        lqw.eq(StringUtils.isNotBlank(bo.getStateCode()), RedemptionEligibilityPolicy::getStateCode, bo.getStateCode());
        lqw.eq(StringUtils.isNotBlank(bo.getChannel()), RedemptionEligibilityPolicy::getChannel, bo.getChannel());
        lqw.eq(StringUtils.isNotBlank(bo.getEffect()), RedemptionEligibilityPolicy::getEffect, bo.getEffect());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), RedemptionEligibilityPolicy::getStatus, bo.getStatus());
        lqw.orderByDesc(RedemptionEligibilityPolicy::getPriority, RedemptionEligibilityPolicy::getCreateTime);
        return lqw;
    }

    private void normalizeDefaults(RedemptionEligibilityPolicy policy) {
        policy.setCurrencyCode(normalize(policy.getCurrencyCode()));
        policy.setCountryCode(normalizeNullable(policy.getCountryCode()));
        policy.setStateCode(normalizeNullable(policy.getStateCode()));
        policy.setChannel(normalizeNullable(policy.getChannel()));
        policy.setEffect(StringUtils.blankToDefault(normalize(policy.getEffect()), EFFECT_DENY));
        policy.setPriority(policy.getPriority() == null ? 0 : policy.getPriority());
        policy.setStatus(StringUtils.blankToDefault(policy.getStatus(), SystemConstants.NORMAL));
    }

    private boolean activeAt(RedemptionEligibilityPolicy policy, Date now) {
        return (policy.getStartTime() == null || !policy.getStartTime().after(now))
            && (policy.getEndTime() == null || !policy.getEndTime().before(now));
    }

    private boolean blankOrEquals(String expected, String actual) {
        return StringUtils.isBlank(expected) || StringUtils.equalsIgnoreCase(expected, actual);
    }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String normalizeNullable(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }
}
