package com.gameluck.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.mybatis.helper.MemberNoQueryHelper;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.wallet.domain.WalletRelease;
import com.gameluck.wallet.domain.bo.WalletReleaseBo;
import com.gameluck.wallet.domain.vo.WalletReleaseVo;
import com.gameluck.wallet.mapper.WalletReleaseMapper;
import com.gameluck.wallet.service.IWalletReleaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Wallet release service implementation.
 */
@RequiredArgsConstructor
@Service
public class WalletReleaseServiceImpl implements IWalletReleaseService {

    private final WalletReleaseMapper baseMapper;

    @Override
    public TableDataInfo<WalletReleaseVo> queryPageList(WalletReleaseBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WalletRelease> lqw = buildQueryWrapper(bo);
        Page<WalletReleaseVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public WalletReleaseVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<WalletReleaseVo> queryList(WalletReleaseBo bo) {
        LambdaQueryWrapper<WalletRelease> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<WalletRelease> buildQueryWrapper(WalletReleaseBo bo) {
        LambdaQueryWrapper<WalletRelease> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), WalletRelease::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getReleaseNo()), WalletRelease::getReleaseNo, bo.getReleaseNo());
        lqw.eq(bo.getMemberId() != null, WalletRelease::getMemberId, bo.getMemberId());
        MemberNoQueryHelper.apply(lqw, bo.getMemberNo(), "gl_wallet_release");
        lqw.eq(StringUtils.isNotBlank(bo.getCurrencyCode()), WalletRelease::getCurrencyCode, bo.getCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getSourceType()), WalletRelease::getSourceType, bo.getSourceType());
        lqw.eq(StringUtils.isNotBlank(bo.getBusinessNo()), WalletRelease::getBusinessNo, bo.getBusinessNo());
        lqw.eq(StringUtils.isNotBlank(bo.getReleaseMode()), WalletRelease::getReleaseMode, bo.getReleaseMode());
        lqw.eq(StringUtils.isNotBlank(bo.getReleaseStatus()), WalletRelease::getReleaseStatus, bo.getReleaseStatus());
        lqw.ge(bo.getBeginTime() != null, WalletRelease::getCreateTime, bo.getBeginTime());
        lqw.le(bo.getEndTime() != null, WalletRelease::getCreateTime, bo.getEndTime());
        lqw.orderByDesc(WalletRelease::getCreateTime);
        return lqw;
    }
}
