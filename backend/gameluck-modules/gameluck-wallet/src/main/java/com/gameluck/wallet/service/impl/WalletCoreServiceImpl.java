package com.gameluck.wallet.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.gameluck.common.core.constant.SystemConstants;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.wallet.domain.WalletAccount;
import com.gameluck.wallet.domain.WalletFreeze;
import com.gameluck.wallet.domain.WalletRelease;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.domain.bo.WalletDebitBo;
import com.gameluck.wallet.domain.bo.WalletFreezeOperationBo;
import com.gameluck.wallet.domain.bo.WalletTurnoverBo;
import com.gameluck.wallet.enums.WalletFreezeStatus;
import com.gameluck.wallet.domain.vo.WalletRuleVo;
import com.gameluck.wallet.enums.WalletOperation;
import com.gameluck.wallet.enums.WalletReleaseMode;
import com.gameluck.wallet.enums.WalletReleaseStatus;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.mapper.WalletAccountMapper;
import com.gameluck.wallet.mapper.WalletFreezeMapper;
import com.gameluck.wallet.mapper.WalletReleaseMapper;
import com.gameluck.wallet.mapper.WalletTransactionMapper;
import com.gameluck.wallet.service.IWalletCoreService;
import com.gameluck.wallet.service.IWalletRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wallet core balance operations implementation.
 */
@RequiredArgsConstructor
@Service
public class WalletCoreServiceImpl implements IWalletCoreService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final String DEFAULT_TENANT_ID = "000000";
    private static final int MONEY_SCALE = 6;
    private static final String RELEASED_COUNT_PREFIX = "releasedCount=";
    private static final Pattern RELEASED_COUNT_PATTERN = Pattern.compile("(^|[;\\s])releasedCount=(\\d+)([;\\s]|$)");

    private final WalletAccountMapper walletAccountMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final WalletReleaseMapper walletReleaseMapper;
    private final WalletFreezeMapper walletFreezeMapper;
    private final IWalletRuleService walletRuleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WalletTransaction credit(WalletCreditBo bo) {
        String tenantId = currentTenantId();
        WalletRuleVo rule = walletRuleService.resolveCreditRule(tenantId, bo.getCurrencyCode(), bo.getSourceType());
        WalletReleaseMode releaseMode = resolveReleaseMode(bo, rule);
        BigDecimal amount = requirePositive(bo.getAmount(), "wallet.credit.amount.positive");
        BigDecimal requiredTurnover = resolveRequiredTurnover(bo, rule);
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
        BigDecimal balanceAfter = normalizeAmount(balanceBefore.add(amount));
        account.setAvailableBalance(balanceAfter);
        account.setUpdateTime(now);
        walletAccountMapper.updateById(account);

        transaction.setBalanceBefore(normalizeAmount(balanceBefore));
        transaction.setBalanceAfter(balanceAfter);
        transaction.setFrozenBefore(normalizeAmount(frozenBefore));
        transaction.setFrozenAfter(normalizeAmount(frozenBefore));
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
        BigDecimal amount = requirePositive(bo.getAmount(), "wallet.debit.amount.positive");
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
            throw new ServiceException(MessageUtils.message("wallet.account.not.exists"));
        }

        BigDecimal balanceBefore = defaultZero(account.getAvailableBalance());
        BigDecimal frozenBefore = defaultZero(account.getFrozenBalance());
        if (balanceBefore.compareTo(amount) < 0) {
            transaction.setBalanceBefore(normalizeAmount(balanceBefore));
            transaction.setBalanceAfter(normalizeAmount(balanceBefore));
            transaction.setFrozenBefore(normalizeAmount(frozenBefore));
            transaction.setFrozenAfter(normalizeAmount(frozenBefore));
            transaction.setStatus(WalletTransactionStatus.FAILED.name());
            transaction.setFailCode("INSUFFICIENT_BALANCE");
            transaction.setFailReason(MessageUtils.message("wallet.balance.insufficient"));
            transaction.setUpdateTime(now);
            walletTransactionMapper.updateById(transaction);
            return transaction;
        }

        BigDecimal balanceAfter = normalizeAmount(balanceBefore.subtract(amount));
        account.setAvailableBalance(balanceAfter);
        account.setUpdateTime(now);
        walletAccountMapper.updateById(account);

        transaction.setBalanceBefore(normalizeAmount(balanceBefore));
        transaction.setBalanceAfter(balanceAfter);
        transaction.setFrozenBefore(normalizeAmount(frozenBefore));
        transaction.setFrozenAfter(normalizeAmount(frozenBefore));
        transaction.setStatus(WalletTransactionStatus.SUCCESS.name());
        transaction.setUpdateTime(now);
        walletTransactionMapper.updateById(transaction);
        return transaction;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WalletTransaction freeze(WalletFreezeOperationBo bo) {
        String tenantId = currentTenantId();
        BigDecimal amount = requirePositive(bo.getAmount(), "wallet.freeze.amount.positive");
        Date now = new Date();
        String freezeNo = StringUtils.blankToDefault(bo.getFreezeNo(), "WF" + IdUtil.getSnowflakeNextIdStr());
        bo.setFreezeNo(freezeNo);
        WalletTransaction transaction = buildFreezeTransaction(tenantId, bo, WalletOperation.FREEZE, amount, now);
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
            markFailed(transaction, ZERO, ZERO, "ACCOUNT_NOT_FOUND", MessageUtils.message("wallet.account.not.exists"), now);
            return transaction;
        }

        BigDecimal balanceBefore = defaultZero(account.getAvailableBalance());
        BigDecimal frozenBefore = defaultZero(account.getFrozenBalance());
        if (balanceBefore.compareTo(amount) < 0) {
            markFailed(transaction, balanceBefore, frozenBefore, "INSUFFICIENT_BALANCE", MessageUtils.message("wallet.balance.insufficient"), now);
            return transaction;
        }

        BigDecimal balanceAfter = normalizeAmount(balanceBefore.subtract(amount));
        BigDecimal frozenAfter = normalizeAmount(frozenBefore.add(amount));
        account.setAvailableBalance(balanceAfter);
        account.setFrozenBalance(frozenAfter);
        account.setUpdateTime(now);
        walletAccountMapper.updateById(account);

        transaction.setBalanceBefore(normalizeAmount(balanceBefore));
        transaction.setBalanceAfter(balanceAfter);
        transaction.setFrozenBefore(normalizeAmount(frozenBefore));
        transaction.setFrozenAfter(frozenAfter);
        transaction.setStatus(WalletTransactionStatus.SUCCESS.name());
        transaction.setUpdateTime(now);
        walletTransactionMapper.updateById(transaction);
        walletFreezeMapper.insert(buildFreezeRecord(tenantId, bo, amount, now));
        return transaction;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WalletTransaction unfreeze(WalletFreezeOperationBo bo) {
        return completeFreeze(bo, WalletOperation.UNFREEZE, WalletFreezeStatus.RELEASED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WalletTransaction settle(WalletFreezeOperationBo bo) {
        return completeFreeze(bo, WalletOperation.SETTLE, WalletFreezeStatus.SETTLED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addValidTurnover(WalletTurnoverBo bo) {
        String tenantId = currentTenantId();
        BigDecimal validTurnoverAmount = requirePositive(bo.getValidTurnoverAmount(), "wallet.turnover.amount.positive");
        Date now = new Date();
        WalletTransaction transaction = buildTurnoverTransaction(tenantId, bo, validTurnoverAmount, now);
        WalletTransaction exists = walletTransactionMapper.selectByIdempotencyKey(tenantId, bo.getIdempotencyKey());
        if (exists != null) {
            return resolveTurnoverIdempotentResult(exists, transaction.getRequestHash());
        }

        WalletTransaction concurrent = reserveTransaction(transaction);
        if (concurrent != null) {
            return resolveTurnoverIdempotentResult(concurrent, transaction.getRequestHash());
        }

        BigDecimal remainTurnover = validTurnoverAmount;
        List<WalletRelease> lockedList = walletReleaseMapper.selectLockedByMemberForUpdate(
            tenantId, bo.getMemberId(), bo.getCurrencyCode(), WalletReleaseStatus.LOCKED.name());

        int releasedCount = 0;
        for (WalletRelease release : lockedList) {
            BigDecimal completed = normalizeAmount(defaultZero(release.getCompletedTurnover()));
            BigDecimal required = normalizeAmount(defaultZero(release.getRequiredTurnover()));
            BigDecimal need = required.subtract(completed).max(ZERO);
            if (remainTurnover.compareTo(ZERO) <= 0 && need.compareTo(ZERO) > 0) {
                continue;
            }
            BigDecimal applied = remainTurnover.min(need);
            BigDecimal completedAfter = normalizeAmount(completed.add(applied));
            remainTurnover = normalizeAmount(remainTurnover.subtract(applied));

            release.setCompletedTurnover(completedAfter);
            if (completedAfter.compareTo(required) >= 0) {
                release.setReleaseStatus(WalletReleaseStatus.RELEASED.name());
                release.setReleasedAmount(normalizeAmount(release.getAmount()));
                releasedCount++;
            }
            release.setUpdateTime(now);
            walletReleaseMapper.updateById(release);
        }
        transaction.setStatus(WalletTransactionStatus.SUCCESS.name());
        transaction.setRemark(turnoverRemark(releasedCount));
        transaction.setUpdateTime(now);
        walletTransactionMapper.updateById(transaction);
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
            throw new ServiceException(MessageUtils.message("wallet.idempotency.conflict"));
        }
        return exists;
    }

    private int resolveTurnoverIdempotentResult(WalletTransaction exists, String expectedRequestHash) {
        if (!StringUtils.equals(exists.getRequestHash(), expectedRequestHash)) {
            throw new ServiceException(MessageUtils.message("wallet.idempotency.conflict"));
        }
        return parseReleasedCount(exists.getRemark());
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
        transaction.setRequiredTurnover(normalizeAmount(requiredTurnover));
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

    private WalletTransaction buildTurnoverTransaction(String tenantId, WalletTurnoverBo bo, BigDecimal amount, Date now) {
        WalletTransaction transaction = baseTransaction(tenantId, bo.getIdempotencyKey(), bo.getMemberId(),
            bo.getCurrencyCode(), bo.getSourceType(), bo.getBusinessNo(), amount, ZERO, ZERO, ZERO, null, null, now);
        transaction.setOperation(WalletOperation.TURNOVER.name());
        transaction.setRequestHash(requestHash(tenantId, bo.getIdempotencyKey(), bo.getMemberId(), bo.getCurrencyCode(),
            WalletOperation.TURNOVER.name(), bo.getSourceType(), bo.getBusinessNo(), amount));
        return transaction;
    }

    private WalletTransaction buildFreezeTransaction(String tenantId, WalletFreezeOperationBo bo, WalletOperation operation,
                                                     BigDecimal amount, Date now) {
        WalletTransaction transaction = baseTransaction(tenantId, bo.getIdempotencyKey(), bo.getMemberId(),
            bo.getCurrencyCode(), bo.getSourceType(), bo.getBusinessNo(), amount, ZERO, ZERO, ZERO,
            bo.getOperatorId(), bo.getRemark(), now);
        transaction.setOperation(operation.name());
        transaction.setRequestHash(requestHash(tenantId, bo.getIdempotencyKey(), bo.getFreezeNo(), bo.getMemberId(),
            bo.getCurrencyCode(), operation.name(), bo.getSourceType(), bo.getBusinessNo(), amount));
        return transaction;
    }

    private WalletFreeze buildFreezeRecord(String tenantId, WalletFreezeOperationBo bo, BigDecimal amount, Date now) {
        WalletFreeze freeze = new WalletFreeze();
        freeze.setId(IdUtil.getSnowflakeNextId());
        freeze.setTenantId(tenantId);
        freeze.setFreezeNo(bo.getFreezeNo());
        freeze.setMemberId(bo.getMemberId());
        freeze.setCurrencyCode(bo.getCurrencyCode());
        freeze.setAmount(normalizeAmount(amount));
        freeze.setSourceType(bo.getSourceType());
        freeze.setBusinessNo(bo.getBusinessNo());
        freeze.setStatus(WalletFreezeStatus.FROZEN.name());
        freeze.setOperatorId(bo.getOperatorId());
        freeze.setRemark(bo.getRemark());
        freeze.setCreateTime(now);
        freeze.setUpdateTime(now);
        return freeze;
    }

    private WalletTransaction completeFreeze(WalletFreezeOperationBo bo, WalletOperation operation, WalletFreezeStatus targetStatus) {
        String tenantId = currentTenantId();
        Date now = new Date();
        WalletFreeze freeze = walletFreezeMapper.selectByFreezeNoForUpdate(tenantId, bo.getFreezeNo());
        if (freeze == null) {
            throw new ServiceException(MessageUtils.message("wallet.freeze.record.not.exists"));
        }
        BigDecimal amount = normalizeAmount(freeze.getAmount());
        WalletTransaction transaction = buildFreezeTransaction(tenantId, bo, operation, amount, now);
        WalletTransaction exists = walletTransactionMapper.selectByIdempotencyKey(tenantId, bo.getIdempotencyKey());
        if (exists != null) {
            return resolveIdempotentResult(exists, transaction.getRequestHash());
        }

        if (!WalletFreezeStatus.FROZEN.name().equals(freeze.getStatus())) {
            throw new ServiceException(MessageUtils.message("wallet.freeze.record.not.frozen"));
        }
        WalletTransaction concurrent = reserveTransaction(transaction);
        if (concurrent != null) {
            return resolveIdempotentResult(concurrent, transaction.getRequestHash());
        }

        WalletAccount account = walletAccountMapper.selectByBizKeyForUpdate(tenantId, freeze.getMemberId(), freeze.getCurrencyCode());
        if (account == null) {
            throw new ServiceException(MessageUtils.message("wallet.account.not.exists"));
        }
        BigDecimal balanceBefore = defaultZero(account.getAvailableBalance());
        BigDecimal frozenBefore = defaultZero(account.getFrozenBalance());
        if (frozenBefore.compareTo(amount) < 0) {
            throw new ServiceException(MessageUtils.message("wallet.frozen.balance.insufficient"));
        }

        BigDecimal balanceAfter = balanceBefore;
        if (WalletOperation.UNFREEZE == operation) {
            balanceAfter = normalizeAmount(balanceBefore.add(amount));
        }
        BigDecimal frozenAfter = normalizeAmount(frozenBefore.subtract(amount));
        account.setAvailableBalance(balanceAfter);
        account.setFrozenBalance(frozenAfter);
        account.setUpdateTime(now);
        walletAccountMapper.updateById(account);

        freeze.setStatus(targetStatus.name());
        freeze.setUpdateTime(now);
        walletFreezeMapper.updateById(freeze);

        transaction.setMemberId(freeze.getMemberId());
        transaction.setCurrencyCode(freeze.getCurrencyCode());
        transaction.setAmount(amount);
        transaction.setBalanceBefore(normalizeAmount(balanceBefore));
        transaction.setBalanceAfter(normalizeAmount(balanceAfter));
        transaction.setFrozenBefore(normalizeAmount(frozenBefore));
        transaction.setFrozenAfter(frozenAfter);
        transaction.setStatus(WalletTransactionStatus.SUCCESS.name());
        transaction.setUpdateTime(now);
        walletTransactionMapper.updateById(transaction);
        return transaction;
    }

    private void markFailed(WalletTransaction transaction, BigDecimal balanceBefore, BigDecimal frozenBefore,
                            String failCode, String failReason, Date now) {
        transaction.setBalanceBefore(normalizeAmount(balanceBefore));
        transaction.setBalanceAfter(normalizeAmount(balanceBefore));
        transaction.setFrozenBefore(normalizeAmount(frozenBefore));
        transaction.setFrozenAfter(normalizeAmount(frozenBefore));
        transaction.setStatus(WalletTransactionStatus.FAILED.name());
        transaction.setFailCode(failCode);
        transaction.setFailReason(failReason);
        transaction.setUpdateTime(now);
        walletTransactionMapper.updateById(transaction);
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
        transaction.setAmount(normalizeAmount(amount));
        transaction.setBalanceBefore(normalizeAmount(balanceBefore));
        transaction.setBalanceAfter(normalizeAmount(balanceAfter));
        transaction.setFrozenBefore(normalizeAmount(frozenBefore));
        transaction.setFrozenAfter(normalizeAmount(frozenBefore));
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
        release.setAmount(normalizeAmount(amount));
        release.setReleasedAmount(initialReleasedAmount(releaseMode, amount, requiredTurnover));
        release.setConsumedAmount(normalizeAmount(ZERO));
        release.setRequiredTurnover(normalizeAmount(requiredTurnover));
        release.setCompletedTurnover(normalizeAmount(ZERO));
        release.setReleaseMode(releaseMode.name());
        release.setReleaseStatus(releaseStatus(releaseMode, requiredTurnover).name());
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
            throw new ServiceException(MessageUtils.message("wallet.release.mode.unsupported", releaseMode));
        }
    }

    private WalletReleaseMode resolveReleaseMode(WalletCreditBo bo, WalletRuleVo rule) {
        WalletReleaseMode ruleMode = parseReleaseMode(rule.getReleaseMode());
        if (StringUtils.isBlank(bo.getReleaseMode())) {
            return ruleMode;
        }
        WalletReleaseMode requestMode = parseReleaseMode(bo.getReleaseMode());
        if (requestMode != ruleMode) {
            throw new ServiceException(MessageUtils.message("wallet.release.mode.not.match.rule"));
        }
        return ruleMode;
    }

    private BigDecimal resolveRequiredTurnover(WalletCreditBo bo, WalletRuleVo rule) {
        BigDecimal requestTurnover = bo.getRequiredTurnover();
        BigDecimal defaultTurnover = defaultZero(rule.getDefaultRequiredTurnover());
        if (SystemConstants.NORMAL.equals(rule.getTurnoverRequired())) {
            return normalizeAmount(requestTurnover == null ? defaultTurnover : requestTurnover);
        }
        return normalizeAmount(requestTurnover == null ? defaultTurnover : requestTurnover);
    }

    private WalletReleaseStatus releaseStatus(WalletReleaseMode releaseMode, BigDecimal requiredTurnover) {
        return switch (releaseMode) {
            case IMMEDIATE -> WalletReleaseStatus.RELEASED;
            case AFTER_TURNOVER -> normalizeAmount(requiredTurnover).compareTo(ZERO) <= 0
                ? WalletReleaseStatus.RELEASED : WalletReleaseStatus.LOCKED;
            case NEVER -> WalletReleaseStatus.NEVER;
            case MANUAL_REVIEW -> WalletReleaseStatus.REVIEWING;
        };
    }

    private BigDecimal initialReleasedAmount(WalletReleaseMode releaseMode, BigDecimal amount, BigDecimal requiredTurnover) {
        if (WalletReleaseMode.IMMEDIATE == releaseMode
            || (WalletReleaseMode.AFTER_TURNOVER == releaseMode && normalizeAmount(requiredTurnover).compareTo(ZERO) <= 0)) {
            return normalizeAmount(amount);
        }
        return normalizeAmount(ZERO);
    }

    private BigDecimal requirePositive(BigDecimal value, String message) {
        BigDecimal normalized = normalizeAmount(value);
        if (normalized == null || normalized.compareTo(ZERO) <= 0) {
            throw new ServiceException(MessageUtils.message(message));
        }
        return normalized;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value == null ? null : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }

    private String requestHash(Object... args) {
        String[] normalizedArgs = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            normalizedArgs[i] = hashValue(args[i]);
        }
        return DigestUtil.sha256Hex(StringUtils.join(normalizedArgs, "|"));
    }

    private String hashValue(Object arg) {
        if (arg instanceof BigDecimal decimal) {
            return normalizeAmount(decimal).toPlainString();
        }
        return arg == null ? "" : String.valueOf(arg);
    }

    private String turnoverRemark(int releasedCount) {
        return RELEASED_COUNT_PREFIX + releasedCount;
    }

    private int parseReleasedCount(String remark) {
        if (StringUtils.isBlank(remark)) {
            return 0;
        }
        Matcher matcher = RELEASED_COUNT_PATTERN.matcher(remark);
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

}
