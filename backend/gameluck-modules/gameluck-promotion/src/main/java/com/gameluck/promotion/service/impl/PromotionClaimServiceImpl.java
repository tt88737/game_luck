package com.gameluck.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.promotion.domain.PromotionClaim;
import com.gameluck.promotion.domain.bo.PromotionClaimBo;
import com.gameluck.promotion.domain.vo.PromotionClaimVo;
import com.gameluck.promotion.mapper.PromotionClaimMapper;
import com.gameluck.promotion.service.IPromotionClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Promotion claim query service implementation.
 */
@RequiredArgsConstructor
@Service
public class PromotionClaimServiceImpl implements IPromotionClaimService {

    private final PromotionClaimMapper baseMapper;

    @Override
    public TableDataInfo<PromotionClaimVo> queryPageList(PromotionClaimBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PromotionClaim> lqw = buildQueryWrapper(bo);
        Page<PromotionClaimVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public List<PromotionClaimVo> queryList(PromotionClaimBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<PromotionClaim> buildQueryWrapper(PromotionClaimBo bo) {
        LambdaQueryWrapper<PromotionClaim> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), PromotionClaim::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getClaimNo()), PromotionClaim::getClaimNo, bo.getClaimNo());
        lqw.eq(bo.getPromotionId() != null, PromotionClaim::getPromotionId, bo.getPromotionId());
        lqw.eq(StringUtils.isNotBlank(bo.getPromotionNo()), PromotionClaim::getPromotionNo, bo.getPromotionNo());
        lqw.eq(bo.getMemberId() != null, PromotionClaim::getMemberId, bo.getMemberId());
        lqw.eq(StringUtils.isNotBlank(bo.getCurrencyCode()), PromotionClaim::getCurrencyCode, bo.getCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), PromotionClaim::getStatus, bo.getStatus());
        lqw.ge(bo.getBeginTime() != null, PromotionClaim::getCreateTime, bo.getBeginTime());
        lqw.le(bo.getEndTime() != null, PromotionClaim::getCreateTime, bo.getEndTime());
        lqw.orderByDesc(PromotionClaim::getCreateTime);
        return lqw;
    }
}
