package com.gameluck.member.service.impl;

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
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.domain.bo.MemberProfileBo;
import com.gameluck.member.domain.vo.MemberProfileVo;
import com.gameluck.member.enums.MemberKycStatus;
import com.gameluck.member.enums.MemberRiskLevel;
import com.gameluck.member.enums.MemberStatus;
import com.gameluck.member.mapper.MemberProfileMapper;
import com.gameluck.member.service.IMemberProfileService;
import com.gameluck.member.service.MemberIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Member profile service implementation.
 */
@RequiredArgsConstructor
@Service
public class MemberProfileServiceImpl implements IMemberProfileService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String DEFAULT_CHANNEL = "ADMIN";
    private static final String DEFAULT_KYC_REVIEWED_BY = "admin";

    private final MemberProfileMapper baseMapper;
    private final MemberIdGenerator memberIdGenerator;

    @Override
    public TableDataInfo<MemberProfileVo> queryPageList(MemberProfileBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<MemberProfile> lqw = buildQueryWrapper(bo);
        Page<MemberProfileVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public MemberProfileVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<MemberProfileVo> queryList(MemberProfileBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(MemberProfileBo bo) {
        String tenantId = currentTenantId();
        String username = normalizeUsername(bo.getUsername());
        requireUsernameAvailable(tenantId, username, null);

        Date now = new Date();
        MemberProfile add = BeanUtil.toBean(bo, MemberProfile.class);
        add.setId(IdUtil.getSnowflakeNextId());
        add.setTenantId(tenantId);
        add.setMemberNo(memberIdGenerator.next());
        add.setUsername(username);
        add.setNickname(StringUtils.blankToDefault(bo.getNickname(), username));
        add.setStatus(StringUtils.blankToDefault(bo.getStatus(), MemberStatus.ACTIVE.name()));
        add.setRiskLevel(StringUtils.blankToDefault(bo.getRiskLevel(), MemberRiskLevel.NORMAL.name()));
        add.setKycStatus(StringUtils.blankToDefault(bo.getKycStatus(), MemberKycStatus.NOT_STARTED.name()));
        add.setRegisterChannel(StringUtils.blankToDefault(bo.getRegisterChannel(), DEFAULT_CHANNEL));
        validateStatus(add.getStatus());
        validateRiskLevel(add.getRiskLevel());
        validateKycStatus(add.getKycStatus());
        add.setVersion(0);
        add.setDelFlag(SystemConstants.NORMAL);
        add.setCreateTime(now);
        add.setUpdateTime(now);
        return baseMapper.insert(add) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(MemberProfileBo bo) {
        MemberProfile member = baseMapper.selectById(bo.getId());
        if (member == null) {
            throw new ServiceException(MessageUtils.message("member.profile.not.exists"));
        }
        String tenantId = StringUtils.blankToDefault(member.getTenantId(), currentTenantId());
        String username = normalizeUsername(bo.getUsername());
        requireUsernameAvailable(tenantId, username, member.getId());
        if (StringUtils.isNotBlank(bo.getStatus())) {
            validateStatus(bo.getStatus());
        }
        if (StringUtils.isNotBlank(bo.getRiskLevel())) {
            validateRiskLevel(bo.getRiskLevel());
        }
        String normalizedKycStatus = StringUtils.blankToDefault(bo.getKycStatus(), MemberKycStatus.NOT_STARTED.name());
        validateKycStatus(normalizedKycStatus);

        MemberProfile update = BeanUtil.toBean(bo, MemberProfile.class);
        update.setUsername(username);
        update.setTenantId(tenantId);
        update.setKycStatus(normalizedKycStatus);
        String currentKycStatus = StringUtils.blankToDefault(member.getKycStatus(), MemberKycStatus.NOT_STARTED.name());
        boolean kycChanged = !normalizedKycStatus.equals(currentKycStatus);
        boolean reasonChanged = !StringUtils.equals(StringUtils.trim(bo.getKycReviewReason()), StringUtils.trim(member.getKycReviewReason()));
        if (kycChanged || reasonChanged) {
            update.setKycReviewedBy(DEFAULT_KYC_REVIEWED_BY);
            update.setKycReviewTime(new Date());
        }
        update.setUpdateTime(new Date());
        return baseMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberProfileVo updateStatus(Long id, String status) {
        validateStatus(status);
        MemberProfile member = baseMapper.selectById(id);
        if (member == null) {
            throw new ServiceException(MessageUtils.message("member.profile.not.exists"));
        }
        member.setStatus(status);
        member.setUpdateTime(new Date());
        baseMapper.updateById(member);
        return BeanUtil.toBean(member, MemberProfileVo.class);
    }

    private LambdaQueryWrapper<MemberProfile> buildQueryWrapper(MemberProfileBo bo) {
        LambdaQueryWrapper<MemberProfile> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), MemberProfile::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getMemberNo()), MemberProfile::getMemberNo, bo.getMemberNo());
        lqw.like(StringUtils.isNotBlank(bo.getUsername()), MemberProfile::getUsername, bo.getUsername());
        lqw.like(StringUtils.isNotBlank(bo.getNickname()), MemberProfile::getNickname, bo.getNickname());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), MemberProfile::getStatus, bo.getStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getRiskLevel()), MemberProfile::getRiskLevel, bo.getRiskLevel());
        lqw.eq(StringUtils.isNotBlank(bo.getKycStatus()), MemberProfile::getKycStatus, bo.getKycStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getRegisterChannel()), MemberProfile::getRegisterChannel, bo.getRegisterChannel());
        lqw.eq(StringUtils.isNotBlank(bo.getCountryCode()), MemberProfile::getCountryCode, bo.getCountryCode());
        lqw.eq(StringUtils.isNotBlank(bo.getStateCode()), MemberProfile::getStateCode, bo.getStateCode());
        lqw.ge(bo.getBeginTime() != null, MemberProfile::getCreateTime, bo.getBeginTime());
        lqw.le(bo.getEndTime() != null, MemberProfile::getCreateTime, bo.getEndTime());
        lqw.orderByDesc(MemberProfile::getCreateTime);
        return lqw;
    }

    private void requireUsernameAvailable(String tenantId, String username, Long currentId) {
        MemberProfile existing = baseMapper.selectByUsername(tenantId, username);
        if (existing != null && !existing.getId().equals(currentId)) {
            throw new ServiceException(MessageUtils.message("member.username.exists"));
        }
    }

    private String normalizeUsername(String username) {
        String normalized = StringUtils.trim(username);
        if (StringUtils.isBlank(normalized)) {
            throw new ServiceException(MessageUtils.message("member.username.required"));
        }
        return normalized;
    }

    private void validateStatus(String status) {
        try {
            MemberStatus.valueOf(status);
        } catch (Exception ex) {
            throw new ServiceException(MessageUtils.message("member.status.invalid"));
        }
    }

    private void validateRiskLevel(String riskLevel) {
        try {
            MemberRiskLevel.valueOf(riskLevel);
        } catch (Exception ex) {
            throw new ServiceException(MessageUtils.message("member.risk.level.invalid"));
        }
    }

    private void validateKycStatus(String kycStatus) {
        try {
            MemberKycStatus.valueOf(kycStatus);
        } catch (Exception ex) {
            throw new ServiceException(MessageUtils.message("member.kyc.status.invalid"));
        }
    }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }
}
