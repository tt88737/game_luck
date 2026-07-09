package com.gameluck.promotion.service.impl;

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
import com.gameluck.promotion.domain.PromotionClaim;
import com.gameluck.promotion.domain.PromotionReward;
import com.gameluck.promotion.domain.bo.PromotionClaimBo;
import com.gameluck.promotion.domain.bo.PromotionRewardBo;
import com.gameluck.promotion.domain.vo.PromotionClaimVo;
import com.gameluck.promotion.domain.vo.PromotionRewardVo;
import com.gameluck.promotion.enums.PromotionClaimStatus;
import com.gameluck.promotion.enums.PromotionRewardStatus;
import com.gameluck.promotion.mapper.PromotionClaimMapper;
import com.gameluck.promotion.mapper.PromotionRewardMapper;
import com.gameluck.promotion.service.IPromotionRewardService;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.enums.WalletReleaseMode;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.service.IWalletCoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Promotion reward service implementation.
 */
@RequiredArgsConstructor
@Service
public class PromotionRewardServiceImpl implements IPromotionRewardService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String DEFAULT_CURRENCY = "SC";
    private static final String SOURCE_TYPE = "PROMOTION";
    private static final int MONEY_SCALE = 6;

    private final PromotionRewardMapper rewardMapper;
    private final PromotionClaimMapper claimMapper;
    private final IWalletCoreService walletCoreService;

    @Override
    public TableDataInfo<PromotionRewardVo> queryPageList(PromotionRewardBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PromotionReward> lqw = buildRewardQueryWrapper(bo);
        Page<PromotionRewardVo> page = rewardMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public PromotionRewardVo queryById(Long id) {
        return rewardMapper.selectVoById(id);
    }

    @Override
    public List<PromotionRewardVo> queryList(PromotionRewardBo bo) {
        return rewardMapper.selectVoList(buildRewardQueryWrapper(bo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(PromotionRewardBo bo) {
        Date now = new Date();
        PromotionReward add = BeanUtil.toBean(bo, PromotionReward.class);
        add.setId(IdUtil.getSnowflakeNextId());
        add.setTenantId(currentTenantId());
        add.setPromotionNo("PR" + IdUtil.getSnowflakeNextIdStr());
        add.setCurrencyCode(StringUtils.blankToDefault(bo.getCurrencyCode(), DEFAULT_CURRENCY));
        add.setRewardAmount(normalizePositive(bo.getRewardAmount()));
        add.setStatus(StringUtils.blankToDefault(bo.getStatus(), PromotionRewardStatus.INACTIVE.name()));
        validateRewardStatus(add.getStatus());
        add.setVersion(0);
        add.setDelFlag(SystemConstants.NORMAL);
        add.setCreateTime(now);
        add.setUpdateTime(now);
        return rewardMapper.insert(add) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(PromotionRewardBo bo) {
        PromotionReward reward = rewardMapper.selectById(bo.getId());
        if (reward == null) {
            throw new ServiceException(MessageUtils.message("promotion.reward.not.exists"));
        }
        PromotionReward update = BeanUtil.toBean(bo, PromotionReward.class);
        update.setCurrencyCode(StringUtils.blankToDefault(bo.getCurrencyCode(), reward.getCurrencyCode()));
        if (bo.getRewardAmount() != null) {
            update.setRewardAmount(normalizePositive(bo.getRewardAmount()));
        }
        if (StringUtils.isNotBlank(bo.getStatus())) {
            validateRewardStatus(bo.getStatus());
        }
        update.setUpdateTime(new Date());
        return rewardMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        return rewardMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromotionRewardVo updateStatus(Long id, String status) {
        validateRewardStatus(status);
        PromotionReward reward = lockReward(id);
        reward.setStatus(status);
        reward.setUpdateTime(new Date());
        rewardMapper.updateById(reward);
        return BeanUtil.toBean(reward, PromotionRewardVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PromotionClaimVo claim(PromotionClaimBo bo) {
        String tenantId = currentTenantId();
        PromotionReward reward = lockReward(bo.getPromotionId());
        validateClaimable(reward);

        PromotionClaim existing = claimMapper.selectByPromotionAndMember(tenantId, reward.getId(), bo.getMemberId());
        if (existing != null) {
            return BeanUtil.toBean(existing, PromotionClaimVo.class);
        }

        Date now = new Date();
        PromotionClaim claim = new PromotionClaim();
        claim.setId(IdUtil.getSnowflakeNextId());
        claim.setTenantId(tenantId);
        claim.setClaimNo("PC" + IdUtil.getSnowflakeNextIdStr());
        claim.setPromotionId(reward.getId());
        claim.setPromotionNo(reward.getPromotionNo());
        claim.setPromotionName(reward.getPromotionName());
        claim.setMemberId(bo.getMemberId());
        claim.setCurrencyCode(reward.getCurrencyCode());
        claim.setRewardAmount(reward.getRewardAmount());
        claim.setStatus(PromotionClaimStatus.SUCCESS.name());
        claim.setIdempotencyKey(claimIdempotencyKey(tenantId, reward.getPromotionNo(), bo.getMemberId()));
        claim.setRemark(bo.getRemark());
        claim.setVersion(0);
        claim.setDelFlag(SystemConstants.NORMAL);
        claim.setCreateTime(now);
        claim.setUpdateTime(now);
        claimMapper.insert(claim);

        WalletTransaction transaction = walletCoreService.credit(buildCreditBo(claim));
        claim.setWalletTransactionNo(transaction.getTransactionNo());
        claim.setUpdateTime(now);
        if (WalletTransactionStatus.SUCCESS.name().equals(transaction.getStatus())) {
            claim.setFailReason(null);
        } else {
            claim.setStatus(PromotionClaimStatus.FAILED.name());
            claim.setFailReason(StringUtils.substring(transaction.getFailReason(), 0, 500));
        }
        claimMapper.updateById(claim);
        return BeanUtil.toBean(claim, PromotionClaimVo.class);
    }

    private LambdaQueryWrapper<PromotionReward> buildRewardQueryWrapper(PromotionRewardBo bo) {
        LambdaQueryWrapper<PromotionReward> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), PromotionReward::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getPromotionNo()), PromotionReward::getPromotionNo, bo.getPromotionNo());
        lqw.like(StringUtils.isNotBlank(bo.getPromotionName()), PromotionReward::getPromotionName, bo.getPromotionName());
        lqw.eq(StringUtils.isNotBlank(bo.getCurrencyCode()), PromotionReward::getCurrencyCode, bo.getCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), PromotionReward::getStatus, bo.getStatus());
        lqw.ge(bo.getBeginTime() != null, PromotionReward::getCreateTime, bo.getBeginTime());
        lqw.le(bo.getEndQueryTime() != null, PromotionReward::getCreateTime, bo.getEndQueryTime());
        lqw.orderByDesc(PromotionReward::getCreateTime);
        return lqw;
    }

    private PromotionReward lockReward(Long id) {
        PromotionReward reward = rewardMapper.selectByIdForUpdate(id);
        if (reward == null) {
            throw new ServiceException(MessageUtils.message("promotion.reward.not.exists"));
        }
        return reward;
    }

    private void validateClaimable(PromotionReward reward) {
        if (!PromotionRewardStatus.ACTIVE.name().equals(reward.getStatus())) {
            throw new ServiceException(MessageUtils.message("promotion.reward.not.active"));
        }
        Date now = new Date();
        if (reward.getStartTime() != null && now.before(reward.getStartTime())) {
            throw new ServiceException(MessageUtils.message("promotion.reward.not.started"));
        }
        if (reward.getEndTime() != null && now.after(reward.getEndTime())) {
            throw new ServiceException(MessageUtils.message("promotion.reward.ended"));
        }
    }

    private void validateRewardStatus(String status) {
        try {
            PromotionRewardStatus.valueOf(status);
        } catch (Exception ex) {
            throw new ServiceException(MessageUtils.message("promotion.reward.status.invalid"));
        }
    }

    private WalletCreditBo buildCreditBo(PromotionClaim claim) {
        WalletCreditBo bo = new WalletCreditBo();
        bo.setIdempotencyKey(claim.getIdempotencyKey());
        bo.setMemberId(claim.getMemberId());
        bo.setCurrencyCode(claim.getCurrencyCode());
        bo.setSourceType(SOURCE_TYPE);
        bo.setBusinessNo(claim.getClaimNo());
        bo.setAmount(claim.getRewardAmount());
        bo.setReleaseMode(WalletReleaseMode.NEVER.name());
        bo.setRequiredTurnover(BigDecimal.ZERO);
        bo.setRemark(MessageUtils.message("promotion.wallet.remark.simulated.reward"));
        return bo;
    }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }

    private String claimIdempotencyKey(String tenantId, String promotionNo, Long memberId) {
        return "promotion:claim:" + tenantId + ":" + promotionNo + ":" + memberId;
    }

    private BigDecimal normalizePositive(BigDecimal value) {
        BigDecimal normalized = value == null ? null : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (normalized == null || normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(MessageUtils.message("promotion.reward.amount.positive"));
        }
        return normalized;
    }
}
