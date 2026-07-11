package com.gameluck.wallet.service.impl;

import cn.hutool.core.util.IdUtil;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.domain.bo.WalletManualAdjustBo;
import com.gameluck.wallet.enums.WalletReleaseMode;
import com.gameluck.wallet.service.IWalletCoreService;
import com.gameluck.wallet.service.IWalletManualAdjustService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Wallet manual adjustment implementation.
 */
@RequiredArgsConstructor
@Service
public class WalletManualAdjustServiceImpl implements IWalletManualAdjustService {

    private static final String SOURCE_TYPE = "MANUAL_ADJUST";
    private static final String BUSINESS_NO_PREFIX = "MA";
    private static final String IDEMPOTENCY_PREFIX = "manual-adjust:";
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final IWalletCoreService walletCoreService;

    @Override
    public WalletTransaction adjust(WalletManualAdjustBo bo) {
        WalletReleaseMode releaseMode = resolveReleaseMode(bo);
        BigDecimal requiredTurnover = resolveRequiredTurnover(bo, releaseMode);
        String businessNo = BUSINESS_NO_PREFIX + IdUtil.getSnowflakeNextIdStr();

        WalletCreditBo creditBo = new WalletCreditBo();
        creditBo.setIdempotencyKey(IDEMPOTENCY_PREFIX + businessNo);
        creditBo.setMemberId(bo.getMemberId());
        creditBo.setCurrencyCode(bo.getCurrencyCode());
        creditBo.setSourceType(SOURCE_TYPE);
        creditBo.setBusinessNo(businessNo);
        creditBo.setAmount(bo.getAmount());
        creditBo.setReleaseMode(releaseMode.name());
        creditBo.setRequiredTurnover(requiredTurnover);
        creditBo.setOperatorId(bo.getOperatorId());
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
        BigDecimal requiredTurnover = bo.getRequiredTurnover();
        if (requiredTurnover == null || requiredTurnover.compareTo(ZERO) <= 0) {
            throw new ServiceException(MessageUtils.message("wallet.manual.adjust.turnover.positive"));
        }
        return requiredTurnover;
    }
}
