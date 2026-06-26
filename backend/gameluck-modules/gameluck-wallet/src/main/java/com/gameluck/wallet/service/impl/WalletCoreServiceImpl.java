package com.gameluck.wallet.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.gameluck.common.core.constant.SystemConstants;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.wallet.domain.WalletAccount;
import com.gameluck.wallet.domain.WalletRelease;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.domain.bo.WalletDebitBo;
import com.gameluck.wallet.domain.bo.WalletTurnoverBo;
import com.gameluck.wallet.enums.WalletOperation;
import com.gameluck.wallet.enums.WalletReleaseMode;
import com.gameluck.wallet.enums.WalletReleaseStatus;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.mapper.WalletAccountMapper;
import com.gameluck.wallet.mapper.WalletReleaseMapper;
import com.gameluck.wallet.mapper.WalletTransactionMapper;
import com.gameluck.wallet.service.IWalletCoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Wallet core balance operations implementation.
 */
@RequiredArgsConstructor
@Service
public class WalletCoreServiceImpl implements IWalletCoreService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final String DEFAULT_TENANT_ID = "000000";

    private final WalletAccountMapper walletAccountMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final WalletReleaseMapper walletReleaseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WalletTransaction credit(WalletCreditBo bo) {
        String tenantId = currentTenantId();
        WalletReleaseMode releaseMode = parseReleaseMode(bo.getReleaseMode());
        BigDecimal amount = requirePositive(bo.getAmount(), "入账金额必须大于0");
        BigDecimal requiredTurnover = defaultZero(bo.getRequiredTurnover());
        Date now = new Date();
        WalletTransaction transaction = buildCreditTransaction(tenantId, bo, releaseMode, amount, requiredTurnover,
            ZERO, ZERO, ZERO, now);
        WalletTransaction exists = walletTransactionMapper.selectByIdempotencyKey(tenantId, bo.getIdempotencyKey());
        if (exists != null) {
            return resolveIdempotentResult(exists, transaction.getRequestHash());
        }

        WalletTransaction concurrent = reserveTransaction(transaction);
        if (concurrent != null) {
            return resolveIdempotentResult(concurrent, transaction.getRequestHash());
        }

        WalletAccount account = getOrCreateAccountForUpdate(tenantId, bo.getMemberId(), bo.getCurrencyCode(), now);

        BigDecimal balanceBefore = defaultZero(account.getAvailableBalance());
        BigDecimal frozenBefore = defaultZero(account.getFrozenBalance());
        BigDecimal balanceAfter = balanceBefore.add(amount);
        account.setAvailableBalance(balanceAfter);
        account.setUpdateTime(now);
        walletAccountMapper.updateById(account);

        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setFrozenBefore(frozenBefore);
        transaction.setFrozenAfter(frozenBefore);
        transaction.setStatus(WalletTransactionStatus.SUCCESS.name());
        transaction.setUpdateTime(now);
        walletTransactionMapper.updateById(transaction);

        walletReleaseMapper.insert(buildRelease(tenantId, bo, releaseMode, amount, requiredTurnover, now));
        return transaction;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WalletTransaction debit(WalletDebitBo bo) {
        String tenantId = currentTenantId();
        BigDecimal amount = requirePositive(bo.getAmount(), "出账金额必须大于0");
        Date now = new Date();
        WalletTransaction transaction = buildDebitTransaction(tenantId, bo, amount, ZERO, ZERO, ZERO, now);
        WalletTransaction exists = walletTransactionMapper.selectByIdempotencyKey(tenantId, bo.getIdempotencyKey());
        if (exists != null) {
            return resolveIdempotentResult(exists, transaction.getRequestHash());
        }

        WalletTransaction concurrent = reserveTransaction(transaction);
        if (concurrent != null) {
            return resolveIdempotentResult(concurrent, transaction.getRequestHash());
        }

        WalletAccount account = walletAccountMapper.selectByBizKeyForUpdate(tenantId, bo.getMemberId(), bo.getCurrencyCode());
        if (account == null) {
            throw new ServiceException("钱包账户不存在");
        }

        BigDecimal balanceBefore = defaultZero(account.getAvailableBalance());
        BigDecimal frozenBefore = defaultZero(account.getFrozenBalance());
        if (balanceBefore.compareTo(amount) < 0) {
            transaction.setBalanceBefore(balanceBefore);
            transaction.setBalanceAfter(balanceBefore);
            transaction.setFrozenBefore(frozenBefore);
            transaction.setFrozenAfter(frozenBefore);
            transaction.setStatus(WalletTransactionStatus.FAILED.name());
            transaction.setFailCode("INSUFFICIENT_BALANCE");
            transaction.setFailReason("钱包余额不足");
            transaction.setUpdateTime(now);
            walletTransactionMapper.updateById(transaction);
            return transaction;
        }

        BigDecimal balanceAfter = balanceBefore.subtract(amount);
        account.setAvailableBalance(balanceAfter);
        account.setUpdateTime(now);
        walletAccountMapper.updateById(account);

        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setFrozenBefore(frozenBefore);
        transaction.setFrozenAfter(frozenBefore);
        transaction.setStatus(WalletTransactionStatus.SUCCESS.name());
        transaction.setUpdateTime(now);
        walletTransactionMapper.updateById(transaction);
        return transaction;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addValidTurnover(WalletTurnoverBo bo) {
        String tenantId = currentTenantId();
        BigDecimal remainTurnover = requirePositive(bo.getValidTurnoverAmount(), "有效流水必须大于0");
        List<WalletRelease> lockedList = walletReleaseMapper.selectLockedByMemberForUpdate(
            tenantId, bo.getMemberId(), bo.getCurrencyCode(), WalletReleaseStatus.LOCKED.name());

        int releasedCount = 0;
        Date now = new Date();
        for (WalletRelease release : lockedList) {
            if (remainTurnover.compareTo(ZERO) <= 0) {
                break;
            }
            BigDecimal completed = defaultZero(release.getCompletedTurnover());
            BigDecimal required = defaultZero(release.getRequiredTurnover());
            BigDecimal need = required.subtract(completed).max(ZERO);
            BigDecimal applied = remainTurnover.min(need);
            BigDecimal completedAfter = completed.add(applied);
            remainTurnover = remainTurnover.subtract(applied);

            release.setCompletedTurnover(completedAfter);
            if (completedAfter.compareTo(required) >= 0) {
                release.setReleaseStatus(WalletReleaseStatus.RELEASED.name());
                release.setReleasedAmount(release.getAmount());
                releasedCount++;
            }
            release.setUpdateTime(now);
            walletReleaseMapper.updateById(release);
        }
        return releasedCount;
    }

    private WalletAccount getOrCreateAccountForUpdate(String tenantId, Long memberId, String currencyCode, Date now) {
        WalletAccount account = walletAccountMapper.selectByBizKeyForUpdate(tenantId, memberId, currencyCode);
        if (account != null) {
            return account;
        }

        WalletAccount newAccount = new WalletAccount();
        newAccount.setId(IdUtil.getSnowflakeNextId());
        newAccount.setTenantId(tenantId);
        newAccount.setMemberId(memberId);
        newAccount.setCurrencyCode(currencyCode);
        newAccount.setAvailableBalance(ZERO);
        newAccount.setFrozenBalance(ZERO);
        newAccount.setStatus(SystemConstants.NORMAL);
        newAccount.setVersion(0);
        newAccount.setDelFlag(SystemConstants.NORMAL);
        newAccount.setCreateTime(now);
        newAccount.setUpdateTime(now);
        try {
            walletAccountMapper.insert(newAccount);
        } catch (DuplicateKeyException ex) {
            WalletAccount concurrent = walletAccountMapper.selectByBizKeyForUpdate(tenantId, memberId, currencyCode);
            if (concurrent != null) {
                return concurrent;
            }
            throw ex;
        }
        return walletAccountMapper.selectByBizKeyForUpdate(tenantId, memberId, currencyCode);
    }

    private WalletTransaction reserveTransaction(WalletTransaction transaction) {
        try {
            walletTransactionMapper.insert(transaction);
            return null;
        } catch (DuplicateKeyException ex) {
            WalletTransaction concurrent = walletTransactionMapper.selectByIdempotencyKey(
                transaction.getTenantId(), transaction.getIdempotencyKey());
            if (concurrent != null) {
                return concurrent;
            }
            throw ex;
        }
    }

    private WalletTransaction resolveIdempotentResult(WalletTransaction exists, String expectedRequestHash) {
        if (!StringUtils.equals(exists.getRequestHash(), expectedRequestHash)) {
            throw new ServiceException("幂等请求参数冲突");
        }
        return exists;
    }

    private WalletTransaction buildCreditTransaction(String tenantId, WalletCreditBo bo, WalletReleaseMode releaseMode,
                                                     BigDecimal amount, BigDecimal requiredTurnover,
                                                     BigDecimal balanceBefore, BigDecimal balanceAfter,
                                                     BigDecimal frozenBefore, Date now) {
        WalletTransaction transaction = baseTransaction(tenantId, bo.getIdempotencyKey(), bo.getMemberId(),
            bo.getCurrencyCode(), bo.getSourceType(), bo.getBusinessNo(), amount, balanceBefore, balanceAfter,
            frozenBefore, bo.getOperatorId(), bo.getRemark(), now);
        transaction.setOperation(WalletOperation.CREDIT.name());
        transaction.setReleaseMode(releaseMode.name());
        transaction.setRequiredTurnover(requiredTurnover);
        transaction.setRequestHash(requestHash(tenantId, bo.getIdempotencyKey(), bo.getMemberId(), bo.getCurrencyCode(),
            WalletOperation.CREDIT.name(), bo.getSourceType(), bo.getBusinessNo(), amount, releaseMode.name(), requiredTurnover));
        return transaction;
    }

    private WalletTransaction buildDebitTransaction(String tenantId, WalletDebitBo bo, BigDecimal amount,
                                                    BigDecimal balanceBefore, BigDecimal balanceAfter,
                                                    BigDecimal frozenBefore, Date now) {
        WalletTransaction transaction = baseTransaction(tenantId, bo.getIdempotencyKey(), bo.getMemberId(),
            bo.getCurrencyCode(), bo.getSourceType(), bo.getBusinessNo(), amount, balanceBefore, balanceAfter,
            frozenBefore, bo.getOperatorId(), bo.getRemark(), now);
        transaction.setOperation(WalletOperation.DEBIT.name());
        transaction.setRequestHash(requestHash(tenantId, bo.getIdempotencyKey(), bo.getMemberId(), bo.getCurrencyCode(),
            WalletOperation.DEBIT.name(), bo.getSourceType(), bo.getBusinessNo(), amount));
        return transaction;
    }

    private WalletTransaction baseTransaction(String tenantId, String idempotencyKey, Long memberId, String currencyCode,
                                              String sourceType, String businessNo, BigDecimal amount,
                                              BigDecimal balanceBefore, BigDecimal balanceAfter,
                                              BigDecimal frozenBefore, Long operatorId, String remark, Date now) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setId(IdUtil.getSnowflakeNextId());
        transaction.setTenantId(tenantId);
        transaction.setTransactionNo("WT" + IdUtil.getSnowflakeNextIdStr());
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setMemberId(memberId);
        transaction.setCurrencyCode(currencyCode);
        transaction.setSourceType(sourceType);
        transaction.setBusinessNo(businessNo);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setFrozenBefore(frozenBefore);
        transaction.setFrozenAfter(frozenBefore);
        transaction.setStatus(WalletTransactionStatus.PENDING.name());
        transaction.setOperatorId(operatorId);
        transaction.setRemark(remark);
        transaction.setCreateTime(now);
        transaction.setUpdateTime(now);
        return transaction;
    }

    private WalletRelease buildRelease(String tenantId, WalletCreditBo bo, WalletReleaseMode releaseMode,
                                       BigDecimal amount, BigDecimal requiredTurnover, Date now) {
        WalletRelease release = new WalletRelease();
        release.setId(IdUtil.getSnowflakeNextId());
        release.setTenantId(tenantId);
        release.setReleaseNo("WR" + IdUtil.getSnowflakeNextIdStr());
        release.setMemberId(bo.getMemberId());
        release.setCurrencyCode(bo.getCurrencyCode());
        release.setSourceType(bo.getSourceType());
        release.setBusinessNo(bo.getBusinessNo());
        release.setAmount(amount);
        release.setReleasedAmount(WalletReleaseMode.IMMEDIATE == releaseMode ? amount : ZERO);
        release.setConsumedAmount(ZERO);
        release.setRequiredTurnover(requiredTurnover);
        release.setCompletedTurnover(ZERO);
        release.setReleaseMode(releaseMode.name());
        release.setReleaseStatus(releaseStatus(releaseMode).name());
        release.setOperatorId(bo.getOperatorId());
        release.setRemark(bo.getRemark());
        release.setCreateTime(now);
        release.setUpdateTime(now);
        release.setVersion(0);
        return release;
    }

    private WalletReleaseMode parseReleaseMode(String releaseMode) {
        try {
            return WalletReleaseMode.valueOf(releaseMode);
        } catch (IllegalArgumentException ex) {
            throw new ServiceException("不支持的释放模式: {}", releaseMode);
        }
    }

    private WalletReleaseStatus releaseStatus(WalletReleaseMode releaseMode) {
        return switch (releaseMode) {
            case IMMEDIATE -> WalletReleaseStatus.RELEASED;
            case AFTER_TURNOVER -> WalletReleaseStatus.LOCKED;
            case NEVER -> WalletReleaseStatus.NEVER;
            case MANUAL_REVIEW -> WalletReleaseStatus.REVIEWING;
        };
    }

    private BigDecimal requirePositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(ZERO) <= 0) {
            throw new ServiceException(message);
        }
        return value;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }

    private String requestHash(Object... args) {
        return DigestUtil.sha256Hex(StringUtils.join(args, "|"));
    }

}
