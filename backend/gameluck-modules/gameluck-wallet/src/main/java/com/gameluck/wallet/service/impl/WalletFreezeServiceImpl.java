package com.gameluck.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.wallet.domain.WalletFreeze;
import com.gameluck.wallet.domain.bo.WalletFreezeBo;
import com.gameluck.wallet.domain.vo.WalletFreezeVo;
import com.gameluck.wallet.mapper.WalletFreezeMapper;
import com.gameluck.wallet.service.IWalletFreezeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Wallet freeze service implementation.
 */
@RequiredArgsConstructor
@Service
public class WalletFreezeServiceImpl implements IWalletFreezeService {

    private final WalletFreezeMapper baseMapper;

    @Override
    public TableDataInfo<WalletFreezeVo> queryPageList(WalletFreezeBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WalletFreeze> lqw = buildQueryWrapper(bo);
        Page<WalletFreezeVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public WalletFreezeVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<WalletFreezeVo> queryList(WalletFreezeBo bo) {
        LambdaQueryWrapper<WalletFreeze> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<WalletFreeze> buildQueryWrapper(WalletFreezeBo bo) {
        LambdaQueryWrapper<WalletFreeze> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), WalletFreeze::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getFreezeNo()), WalletFreeze::getFreezeNo, bo.getFreezeNo());
        lqw.eq(bo.getMemberId() != null, WalletFreeze::getMemberId, bo.getMemberId());
        lqw.eq(StringUtils.isNotBlank(bo.getCurrencyCode()), WalletFreeze::getCurrencyCode, bo.getCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getSourceType()), WalletFreeze::getSourceType, bo.getSourceType());
        lqw.eq(StringUtils.isNotBlank(bo.getBusinessNo()), WalletFreeze::getBusinessNo, bo.getBusinessNo());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), WalletFreeze::getStatus, bo.getStatus());
        lqw.ge(bo.getBeginTime() != null, WalletFreeze::getCreateTime, bo.getBeginTime());
        lqw.le(bo.getEndTime() != null, WalletFreeze::getCreateTime, bo.getEndTime());
        lqw.orderByDesc(WalletFreeze::getCreateTime);
        return lqw;
    }
}
