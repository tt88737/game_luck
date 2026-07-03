package com.gameluck.redemption.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.constant.SystemConstants;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.redemption.domain.RedemptionOrder;
import com.gameluck.redemption.domain.bo.RedemptionOrderBo;
import com.gameluck.redemption.domain.vo.RedemptionOrderVo;
import com.gameluck.redemption.enums.RedemptionOrderStatus;
import com.gameluck.redemption.mapper.RedemptionOrderMapper;
import com.gameluck.redemption.service.IRedemptionOrderService;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletFreezeOperationBo;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.service.IWalletCoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * Redemption order service implementation.
 */
@RequiredArgsConstructor
@Service
public class RedemptionOrderServiceImpl implements IRedemptionOrderService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String DEFAULT_CURRENCY = "RC";
    private static final String DEFAULT_METHOD = "SIMULATED";
    private static final String SOURCE_TYPE = "REDEMPTION";
    private static final int MONEY_SCALE = 6;

    private final RedemptionOrderMapper baseMapper;
    private final IWalletCoreService walletCoreService;

    @Override
    public TableDataInfo<RedemptionOrderVo> queryPageList(RedemptionOrderBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<RedemptionOrder> lqw = buildQueryWrapper(bo);
        Page<RedemptionOrderVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public RedemptionOrderVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<RedemptionOrderVo> queryList(RedemptionOrderBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(RedemptionOrderBo bo) {
        Date now = new Date();
        String orderNo = "RD" + IdUtil.getSnowflakeNextIdStr();
        String freezeNo = "WF" + IdUtil.getSnowflakeNextIdStr();
        RedemptionOrder add = BeanUtil.toBean(bo, RedemptionOrder.class);
        add.setId(IdUtil.getSnowflakeNextId());
        add.setTenantId(currentTenantId());
        add.setRedemptionOrderNo(orderNo);
        add.setCurrencyCode(StringUtils.blankToDefault(bo.getCurrencyCode(), DEFAULT_CURRENCY));
        add.setAmount(normalizePositive(bo.getAmount()));
        add.setRedemptionMethod(StringUtils.blankToDefault(bo.getRedemptionMethod(), DEFAULT_METHOD));
        add.setStatus(RedemptionOrderStatus.PENDING.name());
        add.setFreezeNo(freezeNo);
        add.setFreezeIdempotencyKey(freezeIdempotencyKey(orderNo));
        add.setSettleIdempotencyKey(settleIdempotencyKey(orderNo));
        add.setReleaseIdempotencyKey(releaseIdempotencyKey(orderNo));
        add.setVersion(0);
        add.setDelFlag(SystemConstants.NORMAL);
        add.setCreateTime(now);
        add.setUpdateTime(now);
        baseMapper.insert(add);

        WalletTransaction transaction = walletCoreService.freeze(buildFreezeBo(add, add.getFreezeIdempotencyKey(), add.getFreezeNo()));
        add.setFreezeWalletTransactionNo(transaction.getTransactionNo());
        add.setUpdateTime(now);
        if (WalletTransactionStatus.SUCCESS.name().equals(transaction.getStatus())) {
            add.setFailReason(null);
        } else {
            add.setStatus(RedemptionOrderStatus.FAILED.name());
            add.setFailReason(StringUtils.substring(transaction.getFailReason(), 0, 500));
        }
        return baseMapper.updateById(add) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RedemptionOrderVo approve(Long id, String reason) {
        RedemptionOrder order = lockOrder(id);
        requirePending(order);
        WalletTransaction transaction = walletCoreService.settle(
            buildFreezeBo(order, order.getSettleIdempotencyKey(), order.getFreezeNo()));
        requireWalletSuccess(transaction);

        Date now = new Date();
        order.setStatus(RedemptionOrderStatus.APPROVED.name());
        order.setSettleWalletTransactionNo(transaction.getTransactionNo());
        order.setAuditTime(now);
        order.setAuditReason(reason);
        order.setFailReason(null);
        order.setUpdateTime(now);
        baseMapper.updateById(order);
        return BeanUtil.toBean(order, RedemptionOrderVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RedemptionOrderVo reject(Long id, String reason) {
        RedemptionOrder order = lockOrder(id);
        requirePending(order);
        WalletTransaction transaction = walletCoreService.unfreeze(
            buildFreezeBo(order, order.getReleaseIdempotencyKey(), order.getFreezeNo()));
        requireWalletSuccess(transaction);

        Date now = new Date();
        order.setStatus(RedemptionOrderStatus.REJECTED.name());
        order.setReleaseWalletTransactionNo(transaction.getTransactionNo());
        order.setAuditTime(now);
        order.setAuditReason(reason);
        order.setFailReason(null);
        order.setUpdateTime(now);
        baseMapper.updateById(order);
        return BeanUtil.toBean(order, RedemptionOrderVo.class);
    }

    private LambdaQueryWrapper<RedemptionOrder> buildQueryWrapper(RedemptionOrderBo bo) {
        LambdaQueryWrapper<RedemptionOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), RedemptionOrder::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getRedemptionOrderNo()), RedemptionOrder::getRedemptionOrderNo, bo.getRedemptionOrderNo());
        lqw.eq(bo.getMemberId() != null, RedemptionOrder::getMemberId, bo.getMemberId());
        lqw.eq(StringUtils.isNotBlank(bo.getCurrencyCode()), RedemptionOrder::getCurrencyCode, bo.getCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), RedemptionOrder::getStatus, bo.getStatus());
        lqw.ge(bo.getBeginTime() != null, RedemptionOrder::getCreateTime, bo.getBeginTime());
        lqw.le(bo.getEndTime() != null, RedemptionOrder::getCreateTime, bo.getEndTime());
        lqw.orderByDesc(RedemptionOrder::getCreateTime);
        return lqw;
    }

    private RedemptionOrder lockOrder(Long id) {
        RedemptionOrder order = baseMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw new ServiceException("redemption order does not exist");
        }
        return order;
    }

    private void requirePending(RedemptionOrder order) {
        if (!RedemptionOrderStatus.PENDING.name().equals(order.getStatus())) {
            throw new ServiceException("only pending redemption orders can be operated");
        }
    }

    private void requireWalletSuccess(WalletTransaction transaction) {
        if (!WalletTransactionStatus.SUCCESS.name().equals(transaction.getStatus())) {
            throw new ServiceException("wallet operation failed");
        }
    }

    private WalletFreezeOperationBo buildFreezeBo(RedemptionOrder order, String idempotencyKey, String freezeNo) {
        WalletFreezeOperationBo bo = new WalletFreezeOperationBo();
        bo.setFreezeNo(freezeNo);
        bo.setMemberId(order.getMemberId());
        bo.setCurrencyCode(order.getCurrencyCode());
        bo.setAmount(order.getAmount());
        bo.setSourceType(SOURCE_TYPE);
        bo.setBusinessNo(order.getRedemptionOrderNo());
        bo.setIdempotencyKey(idempotencyKey);
        bo.setRemark("Simulated redemption");
        return bo;
    }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }

    private String freezeIdempotencyKey(String orderNo) {
        return "redemption:freeze:" + orderNo;
    }

    private String settleIdempotencyKey(String orderNo) {
        return "redemption:settle:" + orderNo;
    }

    private String releaseIdempotencyKey(String orderNo) {
        return "redemption:release:" + orderNo;
    }

    private BigDecimal normalizePositive(BigDecimal value) {
        BigDecimal normalized = value == null ? null : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (normalized == null || normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("amount must be greater than 0");
        }
        return normalized;
    }
}
