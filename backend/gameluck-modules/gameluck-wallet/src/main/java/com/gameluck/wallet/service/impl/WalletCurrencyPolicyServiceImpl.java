package com.gameluck.wallet.service.impl;

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
import com.gameluck.wallet.client.domain.vo.ClientWalletCurrencyVo;
import com.gameluck.wallet.domain.WalletCurrency;
import com.gameluck.wallet.domain.WalletCurrencyPolicy;
import com.gameluck.wallet.domain.bo.WalletCurrencyPolicyBo;
import com.gameluck.wallet.domain.vo.WalletCurrencyPolicyVo;
import com.gameluck.wallet.mapper.WalletCurrencyMapper;
import com.gameluck.wallet.mapper.WalletCurrencyPolicyMapper;
import com.gameluck.wallet.service.IWalletCurrencyPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

/**
 * Wallet currency policy service implementation.
 */
@RequiredArgsConstructor
@Service
public class WalletCurrencyPolicyServiceImpl implements IWalletCurrencyPolicyService {

    private static final String DEFAULT_TENANT_ID = "000000";

    private final WalletCurrencyMapper currencyMapper;
    private final WalletCurrencyPolicyMapper policyMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public TableDataInfo<WalletCurrencyPolicyVo> queryPageList(WalletCurrencyPolicyBo bo, PageQuery pageQuery) {
        Page<WalletCurrencyPolicyVo> page = policyMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return TableDataInfo.build(page);
    }

    @Override
    public WalletCurrencyPolicyVo queryById(Long id) {
        return policyMapper.selectVoById(id);
    }

    @Override
    public int insertByBo(WalletCurrencyPolicyBo bo) {
        WalletCurrencyPolicy add = BeanUtil.toBean(bo, WalletCurrencyPolicy.class);
        add.setId(IdUtil.getSnowflakeNextId());
        add.setTenantId(StringUtils.blankToDefault(bo.getTenantId(), currentTenantId()));
        normalizeDefaults(add);
        add.setVersion(0);
        add.setDelFlag(SystemConstants.NORMAL);
        add.setCreateTime(new java.util.Date());
        add.setUpdateTime(add.getCreateTime());
        return policyMapper.insert(add);
    }

    @Override
    public int updateByBo(WalletCurrencyPolicyBo bo) {
        WalletCurrencyPolicy update = BeanUtil.toBean(bo, WalletCurrencyPolicy.class);
        normalizeDefaults(update);
        update.setUpdateTime(new java.util.Date());
        return policyMapper.updateById(update);
    }

    @Override
    public List<ClientWalletCurrencyVo> listClientCurrencies(String tenantId, Long memberId, String channel) {
        MemberCurrencyContext context = loadMemberContext(tenantId, memberId);
        List<WalletCurrencyPolicy> policies = policyMapper.selectList(Wrappers.lambdaQuery(WalletCurrencyPolicy.class)
            .eq(WalletCurrencyPolicy::getTenantId, tenantId)
            .eq(WalletCurrencyPolicy::getStatus, SystemConstants.NORMAL)
            .orderByDesc(WalletCurrencyPolicy::getPriority));
        return currencyMapper.selectList(Wrappers.lambdaQuery(WalletCurrency.class)
                .eq(WalletCurrency::getTenantId, tenantId)
                .eq(WalletCurrency::getEnabled, SystemConstants.NORMAL)
                .orderByAsc(WalletCurrency::getSortOrder, WalletCurrency::getCurrencyCode))
            .stream()
            .map(currency -> toClientCurrency(currency, matchingPolicies(currency, policies, context, channel)))
            .filter(ClientWalletCurrencyVo::getVisibleForPolicy)
            .peek(ClientWalletCurrencyVo::clearVisibleForPolicy)
            .toList();
    }

    protected MemberCurrencyContext loadMemberContext(String tenantId, Long memberId) {
        return jdbcTemplate.queryForObject("""
                SELECT country_code, state_code
                FROM gl_member_profile
                WHERE tenant_id = ? AND id = ? AND del_flag = '0'
                """,
            (rs, rowNum) -> mapContext(rs), tenantId, memberId);
    }

    private LambdaQueryWrapper<WalletCurrencyPolicy> buildQueryWrapper(WalletCurrencyPolicyBo bo) {
        LambdaQueryWrapper<WalletCurrencyPolicy> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), WalletCurrencyPolicy::getTenantId, bo.getTenantId());
        lqw.like(StringUtils.isNotBlank(bo.getPolicyName()), WalletCurrencyPolicy::getPolicyName, bo.getPolicyName());
        lqw.eq(StringUtils.isNotBlank(bo.getCurrencyCode()), WalletCurrencyPolicy::getCurrencyCode, bo.getCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getCountryCode()), WalletCurrencyPolicy::getCountryCode, bo.getCountryCode());
        lqw.eq(StringUtils.isNotBlank(bo.getStateCode()), WalletCurrencyPolicy::getStateCode, bo.getStateCode());
        lqw.eq(StringUtils.isNotBlank(bo.getChannel()), WalletCurrencyPolicy::getChannel, bo.getChannel());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), WalletCurrencyPolicy::getStatus, bo.getStatus());
        lqw.orderByDesc(WalletCurrencyPolicy::getPriority, WalletCurrencyPolicy::getCreateTime);
        return lqw;
    }

    private void normalizeDefaults(WalletCurrencyPolicy policy) {
        policy.setVisibleEnabled(StringUtils.blankToDefault(policy.getVisibleEnabled(), SystemConstants.NORMAL));
        policy.setDepositEnabled(StringUtils.blankToDefault(policy.getDepositEnabled(), SystemConstants.DISABLE));
        policy.setWithdrawEnabled(StringUtils.blankToDefault(policy.getWithdrawEnabled(), SystemConstants.DISABLE));
        policy.setExchangeEnabled(StringUtils.blankToDefault(policy.getExchangeEnabled(), SystemConstants.DISABLE));
        policy.setPlayEnabled(StringUtils.blankToDefault(policy.getPlayEnabled(), SystemConstants.DISABLE));
        policy.setStatus(StringUtils.blankToDefault(policy.getStatus(), SystemConstants.NORMAL));
        policy.setPriority(policy.getPriority() == null ? 0 : policy.getPriority());
    }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }

    private MemberCurrencyContext mapContext(ResultSet rs) throws SQLException {
        return new MemberCurrencyContext(rs.getString("country_code"), rs.getString("state_code"));
    }

    private List<WalletCurrencyPolicy> matchingPolicies(WalletCurrency currency, List<WalletCurrencyPolicy> policies,
                                                        MemberCurrencyContext context, String channel) {
        return policies.stream()
            .filter(policy -> StringUtils.equals(policy.getCurrencyCode(), currency.getCurrencyCode()))
            .filter(policy -> blankOrEquals(policy.getCountryCode(), context.countryCode()))
            .filter(policy -> blankOrEquals(policy.getStateCode(), context.stateCode()))
            .filter(policy -> blankOrEquals(policy.getChannel(), channel))
            .sorted(Comparator.comparing(WalletCurrencyPolicy::getPriority, Comparator.nullsFirst(Integer::compareTo)).reversed())
            .toList();
    }

    private ClientWalletCurrencyVo toClientCurrency(WalletCurrency currency, List<WalletCurrencyPolicy> policies) {
        ClientWalletCurrencyVo vo = new ClientWalletCurrencyVo();
        vo.setCurrencyCode(currency.getCurrencyCode());
        vo.setCurrencyName(currency.getCurrencyName());
        vo.setDecimalScale(currency.getScaleNum());
        boolean visible = enabled("0");
        boolean deposit = enabled(currency.getDepositEnabled());
        boolean withdraw = enabled(currency.getWithdrawEnabled());
        boolean exchange = enabled(currency.getExchangeEnabled());
        boolean play = enabled(currency.getPlayEnabled());
        for (WalletCurrencyPolicy policy : policies) {
            visible = visible && enabled(policy.getVisibleEnabled());
            deposit = deposit && enabled(policy.getDepositEnabled());
            withdraw = withdraw && enabled(policy.getWithdrawEnabled());
            exchange = exchange && enabled(policy.getExchangeEnabled());
            play = play && enabled(policy.getPlayEnabled());
        }
        vo.setVisibleForPolicy(visible);
        vo.setDepositEnabled(deposit);
        vo.setWithdrawEnabled(withdraw);
        vo.setExchangeEnabled(exchange);
        vo.setPlayEnabled(play);
        return vo;
    }

    private boolean blankOrEquals(String expected, String actual) {
        return StringUtils.isBlank(expected) || StringUtils.equalsIgnoreCase(expected, actual);
    }

    private boolean enabled(String flag) {
        return StringUtils.equals(SystemConstants.NORMAL, flag);
    }

    protected record MemberCurrencyContext(String countryCode, String stateCode) {
    }
}
