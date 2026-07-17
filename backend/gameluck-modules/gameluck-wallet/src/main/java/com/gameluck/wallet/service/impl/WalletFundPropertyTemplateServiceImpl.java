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
import com.gameluck.wallet.domain.WalletFundPropertyTemplate;
import com.gameluck.wallet.domain.bo.WalletFundPropertyTemplateBo;
import com.gameluck.wallet.domain.vo.WalletFundPropertyTemplateVo;
import com.gameluck.wallet.mapper.WalletFundPropertyTemplateMapper;
import com.gameluck.wallet.service.IWalletFundPropertyTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@Service
public class WalletFundPropertyTemplateServiceImpl implements IWalletFundPropertyTemplateService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String DISABLED = "1";
    private static final String TURNOVER_MODE_NONE = "NONE";
    private static final String TURNOVER_MODE_FIXED = "FIXED";
    private static final String TURNOVER_MODE_MULTIPLIER = "MULTIPLIER";
    private static final String GAME_SCOPE_ALL = "ALL";
    private static final int MONEY_SCALE = 6;

    private final WalletFundPropertyTemplateMapper baseMapper;

    @Override
    public TableDataInfo<WalletFundPropertyTemplateVo> queryPageList(WalletFundPropertyTemplateBo bo, PageQuery pageQuery) {
        Page<WalletFundPropertyTemplateVo> page = baseMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return TableDataInfo.build(page);
    }

    @Override
    public WalletFundPropertyTemplateVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<WalletFundPropertyTemplateVo> queryList(WalletFundPropertyTemplateBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    public int insertByBo(WalletFundPropertyTemplateBo bo) {
        validateTemplate(bo);
        WalletFundPropertyTemplate add = BeanUtil.toBean(bo, WalletFundPropertyTemplate.class);
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
    public int updateByBo(WalletFundPropertyTemplateBo bo) {
        validateTemplate(bo);
        WalletFundPropertyTemplate update = BeanUtil.toBean(bo, WalletFundPropertyTemplate.class);
        normalizeDefaults(update);
        update.setUpdateTime(new Date());
        return baseMapper.updateById(update);
    }

    private LambdaQueryWrapper<WalletFundPropertyTemplate> buildQueryWrapper(WalletFundPropertyTemplateBo bo) {
        LambdaQueryWrapper<WalletFundPropertyTemplate> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), WalletFundPropertyTemplate::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getPropertyCode()), WalletFundPropertyTemplate::getPropertyCode, bo.getPropertyCode());
        lqw.like(StringUtils.isNotBlank(bo.getPropertyName()), WalletFundPropertyTemplate::getPropertyName, bo.getPropertyName());
        lqw.eq(StringUtils.isNotBlank(bo.getDefaultSourceType()), WalletFundPropertyTemplate::getDefaultSourceType, bo.getDefaultSourceType());
        lqw.eq(StringUtils.isNotBlank(bo.getDefaultTurnoverMode()), WalletFundPropertyTemplate::getDefaultTurnoverMode, bo.getDefaultTurnoverMode());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), WalletFundPropertyTemplate::getStatus, bo.getStatus());
        lqw.orderByAsc(WalletFundPropertyTemplate::getSortOrder, WalletFundPropertyTemplate::getPropertyCode);
        return lqw;
    }

    private void validateTemplate(WalletFundPropertyTemplateBo bo) {
        String turnoverMode = StringUtils.blankToDefault(bo.getDefaultTurnoverMode(), TURNOVER_MODE_NONE);
        if (TURNOVER_MODE_FIXED.equals(turnoverMode)
            && (bo.getDefaultTurnoverRequiredAmount() == null || bo.getDefaultTurnoverRequiredAmount().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new ServiceException(MessageUtils.message("wallet.fund.property.turnover.amount.positive"));
        }
        if (TURNOVER_MODE_MULTIPLIER.equals(turnoverMode)
            && (bo.getDefaultTurnoverMultiplier() == null || bo.getDefaultTurnoverMultiplier().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new ServiceException(MessageUtils.message("wallet.fund.property.turnover.multiplier.positive"));
        }
        if (bo.getDefaultTurnoverRequiredAmount() != null && bo.getDefaultTurnoverRequiredAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException(MessageUtils.message("wallet.fund.property.turnover.amount.invalid"));
        }
        if (bo.getDefaultTurnoverMultiplier() != null && bo.getDefaultTurnoverMultiplier().compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException(MessageUtils.message("wallet.fund.property.turnover.multiplier.invalid"));
        }
    }

    private void normalizeDefaults(WalletFundPropertyTemplate template) {
        if (StringUtils.isNotBlank(template.getPropertyCode())) {
            template.setPropertyCode(template.getPropertyCode().trim().toUpperCase(Locale.ROOT));
        }
        template.setDefaultTurnoverMode(StringUtils.blankToDefault(template.getDefaultTurnoverMode(), TURNOVER_MODE_NONE));
        template.setDefaultGameScopeType(StringUtils.blankToDefault(template.getDefaultGameScopeType(), GAME_SCOPE_ALL));
        template.setDefaultTurnoverRequiredAmount(scale(defaultZero(template.getDefaultTurnoverRequiredAmount()), MONEY_SCALE));
        template.setDefaultTurnoverMultiplier(scale(defaultZero(template.getDefaultTurnoverMultiplier()), 4));
        template.setSortOrder(template.getSortOrder() == null ? 0 : template.getSortOrder());
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
}
