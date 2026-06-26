package com.gameluck.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.wallet.domain.WalletCurrency;
import com.gameluck.wallet.domain.bo.WalletCurrencyBo;
import com.gameluck.wallet.domain.vo.WalletCurrencyVo;
import com.gameluck.wallet.mapper.WalletCurrencyMapper;
import com.gameluck.wallet.service.IWalletCurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Wallet currency service implementation.
 */
@RequiredArgsConstructor
@Service
public class WalletCurrencyServiceImpl implements IWalletCurrencyService {

    private final WalletCurrencyMapper baseMapper;

    @Override
    public TableDataInfo<WalletCurrencyVo> queryPageList(WalletCurrencyBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WalletCurrency> lqw = buildQueryWrapper(bo);
        Page<WalletCurrencyVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public WalletCurrencyVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<WalletCurrencyVo> queryList(WalletCurrencyBo bo) {
        LambdaQueryWrapper<WalletCurrency> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public int updateByBo(WalletCurrencyBo bo) {
        LambdaUpdateWrapper<WalletCurrency> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(WalletCurrency::getId, bo.getId())
            .set(WalletCurrency::getCurrencyName, bo.getCurrencyName())
            .set(WalletCurrency::getScaleNum, bo.getScaleNum())
            .set(WalletCurrency::getEnabled, bo.getEnabled())
            .set(WalletCurrency::getCreditEnabled, bo.getCreditEnabled())
            .set(WalletCurrency::getDebitEnabled, bo.getDebitEnabled())
            .set(WalletCurrency::getFreezeEnabled, bo.getFreezeEnabled())
            .set(WalletCurrency::getWithdrawEnabled, bo.getWithdrawEnabled())
            .set(WalletCurrency::getExchangeEnabled, bo.getExchangeEnabled())
            .set(WalletCurrency::getNegativeAllowed, bo.getNegativeAllowed())
            .set(WalletCurrency::getSortOrder, bo.getSortOrder())
            .set(WalletCurrency::getRemark, bo.getRemark());
        return baseMapper.update(updateWrapper);
    }

    private LambdaQueryWrapper<WalletCurrency> buildQueryWrapper(WalletCurrencyBo bo) {
        LambdaQueryWrapper<WalletCurrency> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), WalletCurrency::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getCurrencyCode()), WalletCurrency::getCurrencyCode, bo.getCurrencyCode());
        lqw.like(StringUtils.isNotBlank(bo.getCurrencyName()), WalletCurrency::getCurrencyName, bo.getCurrencyName());
        lqw.eq(StringUtils.isNotBlank(bo.getEnabled()), WalletCurrency::getEnabled, bo.getEnabled());
        lqw.eq(StringUtils.isNotBlank(bo.getCreditEnabled()), WalletCurrency::getCreditEnabled, bo.getCreditEnabled());
        lqw.eq(StringUtils.isNotBlank(bo.getDebitEnabled()), WalletCurrency::getDebitEnabled, bo.getDebitEnabled());
        lqw.eq(StringUtils.isNotBlank(bo.getFreezeEnabled()), WalletCurrency::getFreezeEnabled, bo.getFreezeEnabled());
        lqw.eq(StringUtils.isNotBlank(bo.getWithdrawEnabled()), WalletCurrency::getWithdrawEnabled, bo.getWithdrawEnabled());
        lqw.eq(StringUtils.isNotBlank(bo.getExchangeEnabled()), WalletCurrency::getExchangeEnabled, bo.getExchangeEnabled());
        lqw.orderByAsc(WalletCurrency::getSortOrder);
        return lqw;
    }
}
