package com.gameluck.game.service.impl;

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
import com.gameluck.game.domain.GameBetOrder;
import com.gameluck.game.domain.bo.GameBetOrderBo;
import com.gameluck.game.domain.vo.GameBetOrderVo;
import com.gameluck.game.enums.GameBetOrderStatus;
import com.gameluck.game.mapper.GameBetOrderMapper;
import com.gameluck.game.service.IGameBetOrderService;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.domain.bo.WalletDebitBo;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.service.IWalletCoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

@RequiredArgsConstructor
@Service
public class GameBetOrderServiceImpl implements IGameBetOrderService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String DEFAULT_CURRENCY = "SC";
    private static final String DEFAULT_GAME_CODE = "SIMULATED";
    private static final int MONEY_SCALE = 6;

    private final GameBetOrderMapper baseMapper;
    private final IWalletCoreService walletCoreService;

    @Override
    public TableDataInfo<GameBetOrderVo> queryPageList(GameBetOrderBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<GameBetOrder> lqw = buildQueryWrapper(bo);
        Page<GameBetOrderVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public GameBetOrderVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(GameBetOrderBo bo) {
        Date now = new Date();
        String orderNo = "GB" + IdUtil.getSnowflakeNextIdStr();
        String roundNo = StringUtils.blankToDefault(bo.getRoundNo(), "ROUND" + IdUtil.getSnowflakeNextIdStr());
        GameBetOrder add = BeanUtil.toBean(bo, GameBetOrder.class);
        add.setId(IdUtil.getSnowflakeNextId());
        add.setTenantId(currentTenantId());
        add.setBetOrderNo(orderNo);
        add.setCurrencyCode(StringUtils.blankToDefault(bo.getCurrencyCode(), DEFAULT_CURRENCY));
        add.setGameCode(StringUtils.blankToDefault(bo.getGameCode(), DEFAULT_GAME_CODE));
        add.setRoundNo(roundNo);
        add.setBetAmount(normalizePositive(bo.getBetAmount(), "betAmount must be greater than 0"));
        add.setPayoutAmount(normalizeNonNegative(bo.getPayoutAmount(), "payoutAmount cannot be negative"));
        add.setNetAmount(normalizeAmount(add.getPayoutAmount().subtract(add.getBetAmount())));
        add.setStatus(GameBetOrderStatus.PENDING.name());
        add.setBetIdempotencyKey(betIdempotencyKey(orderNo));
        add.setSettleIdempotencyKey(settleIdempotencyKey(orderNo));
        add.setRefundIdempotencyKey(refundIdempotencyKey(orderNo));
        add.setVersion(0);
        add.setDelFlag(SystemConstants.NORMAL);
        add.setCreateTime(now);
        add.setUpdateTime(now);
        return baseMapper.insert(add) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameBetOrderVo placeBet(Long id) {
        GameBetOrder order = lockOrder(id);
        if (GameBetOrderStatus.BET_SUCCESS.name().equals(order.getStatus())
            || GameBetOrderStatus.SETTLED.name().equals(order.getStatus())) {
            return BeanUtil.toBean(order, GameBetOrderVo.class);
        }
        requireStatus(order, GameBetOrderStatus.PENDING);

        Date now = new Date();
        WalletTransaction transaction = walletCoreService.debit(buildDebitBo(order));
        order.setBetWalletTransactionNo(transaction.getTransactionNo());
        order.setBetTime(now);
        order.setUpdateTime(now);
        if (WalletTransactionStatus.SUCCESS.name().equals(transaction.getStatus())) {
            order.setStatus(GameBetOrderStatus.BET_SUCCESS.name());
            order.setFailReason(null);
        } else {
            order.setStatus(GameBetOrderStatus.BET_FAILED.name());
            order.setFailReason(StringUtils.substring(transaction.getFailReason(), 0, 500));
        }
        baseMapper.updateById(order);
        return BeanUtil.toBean(order, GameBetOrderVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameBetOrderVo cancel(Long id) {
        GameBetOrder order = lockOrder(id);
        if (GameBetOrderStatus.CANCELLED.name().equals(order.getStatus())) {
            return BeanUtil.toBean(order, GameBetOrderVo.class);
        }
        requireStatus(order, GameBetOrderStatus.BET_SUCCESS);

        Date now = new Date();
        WalletTransaction transaction = walletCoreService.credit(buildRefundCreditBo(order));
        order.setRefundWalletTransactionNo(transaction.getTransactionNo());
        order.setCancelTime(now);
        order.setUpdateTime(now);
        if (WalletTransactionStatus.SUCCESS.name().equals(transaction.getStatus())) {
            order.setStatus(GameBetOrderStatus.CANCELLED.name());
            order.setFailReason(null);
        } else {
            order.setFailReason(StringUtils.substring(transaction.getFailReason(), 0, 500));
        }
        baseMapper.updateById(order);
        return BeanUtil.toBean(order, GameBetOrderVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameBetOrderVo settle(Long id) {
        GameBetOrder order = lockOrder(id);
        if (GameBetOrderStatus.SETTLED.name().equals(order.getStatus())) {
            return BeanUtil.toBean(order, GameBetOrderVo.class);
        }
        requireStatus(order, GameBetOrderStatus.BET_SUCCESS);

        Date now = new Date();
        WalletTransaction transaction = walletCoreService.credit(buildCreditBo(order));
        order.setSettleWalletTransactionNo(transaction.getTransactionNo());
        order.setSettleTime(now);
        order.setUpdateTime(now);
        if (WalletTransactionStatus.SUCCESS.name().equals(transaction.getStatus())) {
            order.setStatus(GameBetOrderStatus.SETTLED.name());
            order.setFailReason(null);
        } else {
            order.setStatus(GameBetOrderStatus.SETTLE_FAILED.name());
            order.setFailReason(StringUtils.substring(transaction.getFailReason(), 0, 500));
        }
        baseMapper.updateById(order);
        return BeanUtil.toBean(order, GameBetOrderVo.class);
    }

    private LambdaQueryWrapper<GameBetOrder> buildQueryWrapper(GameBetOrderBo bo) {
        LambdaQueryWrapper<GameBetOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), GameBetOrder::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getBetOrderNo()), GameBetOrder::getBetOrderNo, bo.getBetOrderNo());
        lqw.eq(bo.getMemberId() != null, GameBetOrder::getMemberId, bo.getMemberId());
        lqw.eq(StringUtils.isNotBlank(bo.getCurrencyCode()), GameBetOrder::getCurrencyCode, bo.getCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getGameCode()), GameBetOrder::getGameCode, bo.getGameCode());
        lqw.eq(StringUtils.isNotBlank(bo.getRoundNo()), GameBetOrder::getRoundNo, bo.getRoundNo());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), GameBetOrder::getStatus, bo.getStatus());
        lqw.ge(bo.getBeginTime() != null, GameBetOrder::getCreateTime, bo.getBeginTime());
        lqw.le(bo.getEndTime() != null, GameBetOrder::getCreateTime, bo.getEndTime());
        lqw.orderByDesc(GameBetOrder::getCreateTime);
        return lqw;
    }

    private GameBetOrder lockOrder(Long id) {
        GameBetOrder order = baseMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw new ServiceException("game bet order does not exist");
        }
        return order;
    }

    private void requireStatus(GameBetOrder order, GameBetOrderStatus expectedStatus) {
        if (!expectedStatus.name().equals(order.getStatus())) {
            throw new ServiceException("invalid game bet order status");
        }
    }

    private WalletDebitBo buildDebitBo(GameBetOrder order) {
        WalletDebitBo debitBo = new WalletDebitBo();
        debitBo.setMemberId(order.getMemberId());
        debitBo.setCurrencyCode(order.getCurrencyCode());
        debitBo.setAmount(order.getBetAmount());
        debitBo.setSourceType("GAME_BET");
        debitBo.setBusinessNo(order.getBetOrderNo());
        debitBo.setIdempotencyKey(order.getBetIdempotencyKey());
        debitBo.setRemark("Simulated game bet");
        return debitBo;
    }

    private WalletCreditBo buildCreditBo(GameBetOrder order) {
        WalletCreditBo creditBo = new WalletCreditBo();
        creditBo.setMemberId(order.getMemberId());
        creditBo.setCurrencyCode(order.getCurrencyCode());
        creditBo.setAmount(order.getPayoutAmount());
        creditBo.setSourceType("GAME_PROFIT");
        creditBo.setBusinessNo(order.getBetOrderNo());
        creditBo.setIdempotencyKey(order.getSettleIdempotencyKey());
        creditBo.setRemark("Simulated game payout");
        return creditBo;
    }

    private WalletCreditBo buildRefundCreditBo(GameBetOrder order) {
        WalletCreditBo creditBo = new WalletCreditBo();
        creditBo.setMemberId(order.getMemberId());
        creditBo.setCurrencyCode(order.getCurrencyCode());
        creditBo.setAmount(order.getBetAmount());
        creditBo.setSourceType("GAME_REFUND");
        creditBo.setBusinessNo(order.getBetOrderNo());
        creditBo.setIdempotencyKey(order.getRefundIdempotencyKey());
        creditBo.setRemark("Simulated game bet refund");
        return creditBo;
    }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }

    private String betIdempotencyKey(String orderNo) {
        return "game:bet:" + orderNo;
    }

    private String settleIdempotencyKey(String orderNo) {
        return "game:settle:" + orderNo;
    }

    private String refundIdempotencyKey(String orderNo) {
        return "game:refund:" + orderNo;
    }

    private BigDecimal normalizePositive(BigDecimal value, String message) {
        BigDecimal normalized = normalizeAmount(value);
        if (normalized == null || normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(message);
        }
        return normalized;
    }

    private BigDecimal normalizeNonNegative(BigDecimal value, String message) {
        BigDecimal normalized = normalizeAmount(value);
        if (normalized == null || normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException(message);
        }
        return normalized;
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value == null ? null : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
