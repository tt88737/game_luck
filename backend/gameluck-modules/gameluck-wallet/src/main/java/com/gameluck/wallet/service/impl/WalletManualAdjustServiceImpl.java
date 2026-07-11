package com.gameluck.wallet.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.satoken.utils.LoginHelper;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.domain.bo.WalletManualAdjustBo;
import com.gameluck.wallet.enums.WalletReleaseMode;
import com.gameluck.wallet.service.IWalletCoreService;
import com.gameluck.wallet.service.IWalletManualAdjustService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Wallet manual adjustment implementation.
 */
@RequiredArgsConstructor
@Service
public class WalletManualAdjustServiceImpl implements IWalletManualAdjustService {

    private static final String SOURCE_TYPE = "MANUAL_ADJUST";
    private static final String IDEMPOTENCY_PREFIX = "manual-adjust:";
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int MONEY_SCALE = 6;

    private final IWalletCoreService walletCoreService;

    @Override
    public WalletTransaction adjust(WalletManualAdjustBo bo) {
        WalletReleaseMode releaseMode = resolveReleaseMode(bo);
        BigDecimal requiredTurnover = resolveRequiredTurnover(bo, releaseMode);
        String adjustmentNo = bo.getAdjustmentNo();

        WalletCreditBo creditBo = new WalletCreditBo();
        creditBo.setIdempotencyKey(IDEMPOTENCY_PREFIX + adjustmentNo);
        creditBo.setMemberId(bo.getMemberId());
        creditBo.setCurrencyCode(bo.getCurrencyCode());
        creditBo.setSourceType(SOURCE_TYPE);
        creditBo.setBusinessNo(adjustmentNo);
        creditBo.setAmount(bo.getAmount());
        creditBo.setReleaseMode(releaseMode.name());
        creditBo.setRequiredTurnover(requiredTurnover);
        creditBo.setManualAdjustOverride(true);
        creditBo.setOperatorId(currentOperatorId());
        creditBo.setRemark(bo.getReason());
        return walletCoreService.credit(creditBo);
    }

    private WalletReleaseMode resolveReleaseMode(WalletManualAdjustBo bo) {
        String strategy = bo.getStrategy();
        if (StringUtils.isBlank(strategy)) {
            throw new ServiceException(MessageUtils.message("wallet.manual.adjust.strategy.invalid"));
        }
        try {
            return switch (strategy.trim().toUpperCase(Locale.ROOT)) {
                case "IMMEDIATE" -> WalletReleaseMode.IMMEDIATE;
                case "AFTER_TURNOVER" -> WalletReleaseMode.AFTER_TURNOVER;
                case "MANUAL_REVIEW" -> WalletReleaseMode.MANUAL_REVIEW;
                default -> throw new IllegalArgumentException(strategy);
            };
        } catch (IllegalArgumentException ex) {
            throw new ServiceException(MessageUtils.message("wallet.manual.adjust.strategy.invalid"));
        }
    }

    private BigDecimal resolveRequiredTurnover(WalletManualAdjustBo bo, WalletReleaseMode releaseMode) {
        if (WalletReleaseMode.AFTER_TURNOVER != releaseMode) {
            return ZERO;
        }
        BigDecimal requiredTurnover = normalizeAmount(bo.getRequiredTurnover());
        if (requiredTurnover == null || requiredTurnover.compareTo(ZERO) <= 0) {
            throw new ServiceException(MessageUtils.message("wallet.manual.adjust.turnover.positive"));
        }
        return requiredTurnover;
    }

    protected Long currentOperatorId() {
        return LoginHelper.getUserId();
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value == null ? null : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
