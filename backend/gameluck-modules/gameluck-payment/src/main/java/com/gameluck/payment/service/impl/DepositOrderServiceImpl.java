package com.gameluck.payment.service.impl;

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
import com.gameluck.payment.domain.DepositOrder;
import com.gameluck.payment.domain.bo.DepositOrderBo;
import com.gameluck.payment.domain.vo.DepositOrderVo;
import com.gameluck.payment.enums.DepositOrderStatus;
import com.gameluck.payment.mapper.DepositOrderMapper;
import com.gameluck.payment.service.IDepositOrderService;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
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
 * Deposit order service implementation.
 */
@RequiredArgsConstructor
@Service
public class DepositOrderServiceImpl implements IDepositOrderService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String DEFAULT_CURRENCY = "RC";
    private static final String SIMULATED = "SIMULATED";
    private static final int MONEY_SCALE = 6;

    private final DepositOrderMapper baseMapper;
    private final IWalletCoreService walletCoreService;

    @Override
    public TableDataInfo<DepositOrderVo> queryPageList(DepositOrderBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DepositOrder> lqw = buildQueryWrapper(bo);
        Page<DepositOrderVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public DepositOrderVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<DepositOrderVo> queryList(DepositOrderBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(DepositOrderBo bo) {
        Date now = new Date();
        String orderNo = "DP" + IdUtil.getSnowflakeNextIdStr();
        String tenantId = currentTenantId();
        DepositOrder add = BeanUtil.toBean(bo, DepositOrder.class);
        add.setId(IdUtil.getSnowflakeNextId());
        add.setTenantId(tenantId);
        add.setDepositOrderNo(orderNo);
        add.setCurrencyCode(StringUtils.blankToDefault(bo.getCurrencyCode(), DEFAULT_CURRENCY));
        add.setAmount(normalizeAmount(bo.getAmount()));
        add.setPayMethod(StringUtils.blankToDefault(bo.getPayMethod(), SIMULATED));
        add.setPayChannel(StringUtils.blankToDefault(bo.getPayChannel(), SIMULATED));
        add.setStatus(DepositOrderStatus.PENDING.name());
        add.setWalletIdempotencyKey(walletIdempotencyKey(orderNo));
        add.setVersion(0);
        add.setDelFlag(SystemConstants.NORMAL);
        add.setCreateTime(now);
        add.setUpdateTime(now);
        return baseMapper.insert(add) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepositOrderVo simulateSuccess(Long id) {
        DepositOrder order = lockOrder(id);
        if (DepositOrderStatus.SUCCESS.name().equals(order.getStatus())) {
            return BeanUtil.toBean(order, DepositOrderVo.class);
        }
        requirePending(order);

        Date now = new Date();
        try {
            WalletTransaction transaction = walletCoreService.credit(buildCreditBo(order));
            if (!WalletTransactionStatus.SUCCESS.name().equals(transaction.getStatus())) {
                throw new ServiceException(MessageUtils.message("payment.deposit.wallet.credit.fail"));
            }
            order.setStatus(DepositOrderStatus.SUCCESS.name());
            order.setWalletTransactionNo(transaction.getTransactionNo());
            order.setPayTime(now);
            order.setFailReason(null);
            order.setUpdateTime(now);
            baseMapper.updateById(order);
            return BeanUtil.toBean(order, DepositOrderVo.class);
        } catch (RuntimeException ex) {
            order.setStatus(DepositOrderStatus.FAILED.name());
            order.setFailReason(StringUtils.substring(ex.getMessage(), 0, 500));
            order.setUpdateTime(now);
            baseMapper.updateById(order);
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancel(Long id) {
        DepositOrder order = lockOrder(id);
        requirePending(order);
        order.setStatus(DepositOrderStatus.CANCELLED.name());
        order.setUpdateTime(new Date());
        return baseMapper.updateById(order) > 0;
    }

    private LambdaQueryWrapper<DepositOrder> buildQueryWrapper(DepositOrderBo bo) {
        LambdaQueryWrapper<DepositOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), DepositOrder::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getDepositOrderNo()), DepositOrder::getDepositOrderNo, bo.getDepositOrderNo());
        lqw.eq(bo.getMemberId() != null, DepositOrder::getMemberId, bo.getMemberId());
        lqw.eq(StringUtils.isNotBlank(bo.getCurrencyCode()), DepositOrder::getCurrencyCode, bo.getCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), DepositOrder::getStatus, bo.getStatus());
        lqw.ge(bo.getBeginTime() != null, DepositOrder::getCreateTime, bo.getBeginTime());
        lqw.le(bo.getEndTime() != null, DepositOrder::getCreateTime, bo.getEndTime());
        lqw.orderByDesc(DepositOrder::getCreateTime);
        return lqw;
    }

    private DepositOrder lockOrder(Long id) {
        DepositOrder order = baseMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw new ServiceException(MessageUtils.message("payment.deposit.order.not.exists"));
        }
        return order;
    }

    private void requirePending(DepositOrder order) {
        if (!DepositOrderStatus.PENDING.name().equals(order.getStatus())) {
            throw new ServiceException(MessageUtils.message("payment.deposit.only.pending.allowed"));
        }
    }

    private WalletCreditBo buildCreditBo(DepositOrder order) {
        WalletCreditBo creditBo = new WalletCreditBo();
        creditBo.setMemberId(order.getMemberId());
        creditBo.setCurrencyCode(order.getCurrencyCode());
        creditBo.setAmount(order.getAmount());
        creditBo.setSourceType("DEPOSIT");
        creditBo.setBusinessNo(order.getDepositOrderNo());
        creditBo.setIdempotencyKey(order.getWalletIdempotencyKey());
        creditBo.setRemark("Simulated deposit success");
        return creditBo;
    }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }

    private String walletIdempotencyKey(String orderNo) {
        return "deposit:success:" + orderNo;
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(MessageUtils.message("payment.deposit.amount.positive"));
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
