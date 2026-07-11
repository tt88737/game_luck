package com.gameluck.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.mapper.MemberProfileMapper;
import com.gameluck.promotion.domain.PromotionClaim;
import com.gameluck.promotion.domain.bo.PromotionClaimBo;
import com.gameluck.promotion.domain.vo.PromotionClaimVo;
import com.gameluck.promotion.mapper.PromotionClaimMapper;
import com.gameluck.promotion.service.IPromotionClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Promotion claim query service implementation.
 */
@RequiredArgsConstructor
@Service
public class PromotionClaimServiceImpl implements IPromotionClaimService {

    private final PromotionClaimMapper baseMapper;
    private final MemberProfileMapper memberProfileMapper;

    @Override
    public TableDataInfo<PromotionClaimVo> queryPageList(PromotionClaimBo bo, PageQuery pageQuery) {
        normalizeMemberQuery(bo);
        LambdaQueryWrapper<PromotionClaim> lqw = buildQueryWrapper(bo);
        Page<PromotionClaimVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        fillMemberNos(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public List<PromotionClaimVo> queryList(PromotionClaimBo bo) {
        normalizeMemberQuery(bo);
        List<PromotionClaimVo> records = baseMapper.selectVoList(buildQueryWrapper(bo));
        fillMemberNos(records);
        return records;
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

    private void normalizeMemberQuery(PromotionClaimBo bo) {
        if (StringUtils.isBlank(bo.getMemberNo())) {
            return;
        }
        MemberProfile member = memberProfileMapper.selectByMemberNo(currentTenantId(), bo.getMemberNo().trim());
        if (member == null) {
            bo.setMemberId(-1L);
            return;
        }
        bo.setMemberId(member.getId());
    }

    private void fillMemberNos(List<PromotionClaimVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> memberIds = records.stream()
            .map(PromotionClaimVo::getMemberId)
            .filter(id -> id != null)
            .distinct()
            .toList();
        if (memberIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<MemberProfile> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MemberProfile::getTenantId, currentTenantId());
        wrapper.in(MemberProfile::getId, memberIds);
        Map<Long, MemberProfile> members = memberProfileMapper.selectList(wrapper).stream()
            .collect(Collectors.toMap(MemberProfile::getId, Function.identity(), (left, right) -> left));
        records.forEach(record -> {
            MemberProfile member = members.get(record.getMemberId());
            if (member != null) {
                record.setMemberNo(member.getMemberNo());
            }
        });
    }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? "000000" : tenantId;
    }
}
