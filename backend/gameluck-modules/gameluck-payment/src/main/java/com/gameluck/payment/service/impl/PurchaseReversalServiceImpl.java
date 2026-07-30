package com.gameluck.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.mapper.MemberProfileMapper;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.domain.PurchaseOrderGrantSnapshot;
import com.gameluck.payment.domain.PurchaseReversal;
import com.gameluck.payment.domain.PurchaseReversalItem;
import com.gameluck.payment.domain.bo.PurchasePaymentCallbackBo;
import com.gameluck.payment.domain.vo.PurchaseReversalResult;
import com.gameluck.payment.enums.PurchaseOrderStatus;
import com.gameluck.payment.enums.PurchasePaymentEventType;
import com.gameluck.payment.enums.PurchaseReversalStatus;
import com.gameluck.payment.enums.PurchaseReversalDispositionStatus;
import com.gameluck.payment.enums.PurchaseReversalType;
import com.gameluck.payment.mapper.PurchaseOrderGrantSnapshotMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.mapper.PurchaseReversalItemMapper;
import com.gameluck.payment.mapper.PurchaseReversalMapper;
import com.gameluck.payment.service.IPurchaseReversalService;
import com.gameluck.wallet.domain.bo.WalletBatchDebitBo;
import com.gameluck.wallet.domain.bo.WalletBatchDebitLineBo;
import com.gameluck.wallet.domain.vo.WalletBatchDebitLineResult;
import com.gameluck.wallet.domain.vo.WalletBatchDebitResult;
import com.gameluck.wallet.service.IWalletCoreService;
import com.gameluck.wallet.service.IWalletTurnoverTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@RequiredArgsConstructor
@Service
public class PurchaseReversalServiceImpl implements IPurchaseReversalService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(6);

    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderGrantSnapshotMapper snapshotMapper;
    private final PurchaseReversalMapper reversalMapper;
    private final PurchaseReversalItemMapper itemMapper;
    private final IWalletCoreService walletCoreService;
    private final IWalletTurnoverTaskService turnoverTaskService;
    private final MemberProfileMapper memberProfileMapper;

    @Override
    public PurchaseReversalResult reverse(PurchaseOrder order, PurchasePaymentCallbackBo callback, Date processingTime) {
        PurchaseReversal existing = reversalMapper.selectByEventKey(order.getTenantId(), callback.getEventKey());
        if (existing != null) {
            String processResult = PurchaseReversalStatus.REVIEW_REQUIRED.name().equals(existing.getStatus())
                ? "REVIEW_REQUIRED" : "OK";
            return new PurchaseReversalResult(order, processResult);
        }
        requireReversible(order);
        Map<String, BigDecimal> amounts = aggregateSnapshots(order);
        boolean chargeback = PurchasePaymentEventType.CHARGEBACK.equals(callback.getEventType());
        String reversalNo = "PRV" + IdUtil.getSnowflakeNextIdStr();
        PurchaseReversal reversal = createReversal(order, callback, processingTime, reversalNo, chargeback);
        reversalMapper.insert(reversal);

        Map<String, PurchaseReversalItem> items = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : amounts.entrySet()) {
            PurchaseReversalItem item = createItem(order, reversal, entry.getKey(), entry.getValue(), processingTime);
            itemMapper.insert(item);
            items.put(entry.getKey(), item);
        }

        WalletBatchDebitResult walletResult = walletCoreService.batchDebit(batchRequest(order, reversalNo, amounts));
        Map<String, WalletBatchDebitLineResult> walletLines = new TreeMap<>();
        for (WalletBatchDebitLineResult line : walletResult.getLines()) {
            walletLines.put(line.getCurrencyCode(), line);
        }
        boolean completed = PurchaseReversalStatus.COMPLETED.name().equals(walletResult.getStatus());
        for (Map.Entry<String, PurchaseReversalItem> entry : items.entrySet()) {
            WalletBatchDebitLineResult line = walletLines.get(entry.getKey());
            PurchaseReversalItem item = entry.getValue();
            item.setAvailableAmount(scale(line.getAvailableAmount()));
            item.setRecoveredAmount(scale(line.getRecoveredAmount()));
            item.setShortfallAmount(scale(line.getShortfallAmount()));
            item.setWalletTransactionNo(line.getWalletTransactionNo());
            item.setStatus(completed ? PurchaseReversalStatus.COMPLETED.name() : PurchaseReversalStatus.REVIEW_REQUIRED.name());
            item.setUpdateTime(processingTime);
            itemMapper.updateById(item);
        }

        if (completed) {
            turnoverTaskService.cancelPendingByPurchase(order.getTenantId(), order.getMemberId(),
                order.getPurchaseOrderNo(), reversalNo, processingTime);
        }
        String reviewReason = completed ? null : MessageUtils.message("payment.purchase.reversal.review.required");
        reversal.setStatus(completed ? PurchaseReversalStatus.COMPLETED.name() : PurchaseReversalStatus.REVIEW_REQUIRED.name());
        reversal.setDispositionStatus(completed ? null : PurchaseReversalDispositionStatus.PENDING_REVIEW.name());
        reversal.setReviewReason(reviewReason);
        reversal.setCompletedTime(completed ? processingTime : null);
        reversal.setUpdateTime(processingTime);
        reversalMapper.updateById(reversal);

        if (chargeback) {
            raiseChargebackRisk(order, callback, reversalNo, processingTime);
        }

        order.setStatus(targetOrderStatus(chargeback, completed).name());
        order.setCallbackEventKey(callback.getEventKey());
        order.setFailReason(reviewReason);
        order.setUpdateTime(processingTime);
        if (chargeback) {
            order.setChargebackTime(processingTime);
        } else {
            order.setRefundTime(processingTime);
        }
        orderMapper.updateById(order);
        return new PurchaseReversalResult(order, completed ? "OK" : "REVIEW_REQUIRED");
    }

    private void raiseChargebackRisk(PurchaseOrder order, PurchasePaymentCallbackBo callback,
                                     String reversalNo, Date processingTime) {
        MemberProfile member = memberProfileMapper.selectByIdForUpdate(order.getTenantId(), order.getMemberId());
        if (member == null) {
            throw new ServiceException(MessageUtils.message("member.not.exists"));
        }
        String reason = MessageUtils.message("payment.purchase.chargeback.risk.reason");
        String source = "PURCHASE_CHARGEBACK:" + reversalNo + ":" + callback.getEventKey();
        if (memberProfileMapper.updateChargebackRisk(order.getTenantId(), order.getMemberId(), reason,
            source, processingTime) != 1) {
            throw new ServiceException(MessageUtils.message("member.not.exists"));
        }
    }

    private Map<String, BigDecimal> aggregateSnapshots(PurchaseOrder order) {
        List<PurchaseOrderGrantSnapshot> snapshots = snapshotMapper.selectByPurchaseOrderNo(order.getTenantId(), order.getPurchaseOrderNo());
        if (snapshots == null || snapshots.isEmpty()) {
            throw new ServiceException(MessageUtils.message("payment.purchase.reversal.snapshot.missing"));
        }
        Map<String, BigDecimal> amounts = new TreeMap<>();
        for (PurchaseOrderGrantSnapshot snapshot : snapshots) {
            String currency = StringUtils.blankToDefault(snapshot.getCurrencyCode(), "").trim().toUpperCase(Locale.ROOT);
            BigDecimal amount = snapshot.getGrantAmount();
            if (StringUtils.isBlank(currency) || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException(MessageUtils.message("payment.purchase.reversal.amount.invalid"));
            }
            amounts.merge(currency, amount, BigDecimal::add);
        }
        amounts.replaceAll((currency, amount) -> scale(amount));
        return amounts;
    }

    private void requireReversible(PurchaseOrder order) {
        if (!PurchaseOrderStatus.PAID.name().equals(order.getStatus())
            && !PurchaseOrderStatus.CREDITED.name().equals(order.getStatus())) {
            throw new ServiceException(MessageUtils.message("payment.purchase.order.status.invalid"));
        }
    }

    private PurchaseReversal createReversal(PurchaseOrder order, PurchasePaymentCallbackBo callback, Date now,
                                             String reversalNo, boolean chargeback) {
        PurchaseReversal reversal = new PurchaseReversal();
        reversal.setId(IdUtil.getSnowflakeNextId());
        reversal.setTenantId(order.getTenantId());
        reversal.setReversalNo(reversalNo);
        reversal.setPurchaseOrderId(order.getId());
        reversal.setPurchaseOrderNo(order.getPurchaseOrderNo());
        reversal.setMemberId(order.getMemberId());
        reversal.setEventKey(callback.getEventKey());
        reversal.setReversalType(chargeback ? PurchaseReversalType.CHARGEBACK.name() : PurchaseReversalType.REFUND.name());
        reversal.setStatus(PurchaseReversalStatus.PROCESSING.name());
        reversal.setReason(callback.getFailReason());
        reversal.setCreateTime(now);
        reversal.setUpdateTime(now);
        return reversal;
    }

    private PurchaseReversalItem createItem(PurchaseOrder order, PurchaseReversal reversal, String currency,
                                             BigDecimal amount, Date now) {
        PurchaseReversalItem item = new PurchaseReversalItem();
        item.setId(IdUtil.getSnowflakeNextId());
        item.setTenantId(order.getTenantId());
        item.setReversalId(reversal.getId());
        item.setReversalNo(reversal.getReversalNo());
        item.setPurchaseOrderNo(order.getPurchaseOrderNo());
        item.setMemberId(order.getMemberId());
        item.setCurrencyCode(currency);
        item.setRequiredAmount(amount);
        item.setAvailableAmount(ZERO);
        item.setRecoveredAmount(ZERO);
        item.setShortfallAmount(ZERO);
        item.setStatus(PurchaseReversalStatus.PROCESSING.name());
        item.setCreateTime(now);
        item.setUpdateTime(now);
        return item;
    }

    private WalletBatchDebitBo batchRequest(PurchaseOrder order, String reversalNo, Map<String, BigDecimal> amounts) {
        WalletBatchDebitBo request = new WalletBatchDebitBo();
        request.setTenantId(order.getTenantId());
        request.setMemberId(order.getMemberId());
        request.setBusinessNo(reversalNo);
        request.setSourceType("PURCHASE_REVERSAL");
        request.setRemark("Purchase reversal " + reversalNo);
        List<WalletBatchDebitLineBo> lines = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : amounts.entrySet()) {
            WalletBatchDebitLineBo line = new WalletBatchDebitLineBo();
            line.setCurrencyCode(entry.getKey());
            line.setAmount(entry.getValue());
            line.setIdempotencyKey("purchase-reversal:" + reversalNo + ":" + entry.getKey());
            lines.add(line);
        }
        request.setLines(lines);
        return request;
    }

    private PurchaseOrderStatus targetOrderStatus(boolean chargeback, boolean completed) {
        if (chargeback) {
            return completed ? PurchaseOrderStatus.CHARGEBACK : PurchaseOrderStatus.CHARGEBACK_REVIEW;
        }
        return completed ? PurchaseOrderStatus.REFUNDED : PurchaseOrderStatus.REFUND_REVIEW;
    }

    private BigDecimal scale(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(6, RoundingMode.HALF_UP);
    }
}
