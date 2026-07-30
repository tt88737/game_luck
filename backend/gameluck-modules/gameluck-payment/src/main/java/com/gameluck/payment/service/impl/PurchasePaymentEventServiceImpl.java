package com.gameluck.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.payment.domain.PurchaseOfferGrantItem;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.domain.PurchasePaymentEvent;
import com.gameluck.payment.domain.bo.PurchasePaymentCallbackBo;
import com.gameluck.payment.domain.vo.PurchaseReversalResult;
import com.gameluck.payment.enums.PurchaseOrderStatus;
import com.gameluck.payment.enums.PurchasePaymentEventStatus;
import com.gameluck.payment.enums.PurchasePaymentEventType;
import com.gameluck.payment.mapper.PurchaseOfferGrantItemMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.mapper.PurchasePaymentEventMapper;
import com.gameluck.payment.service.IPurchaseOfferService;
import com.gameluck.payment.service.IPurchasePaymentEventService;
import com.gameluck.payment.service.IPurchaseReversalService;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.service.IWalletCoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;

@RequiredArgsConstructor
@Service
public class PurchasePaymentEventServiceImpl implements IPurchasePaymentEventService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String DEFAULT_PROVIDER = "SIMULATED";

    private final PurchaseOrderMapper orderMapper;
    private final PurchasePaymentEventMapper eventMapper;
    private final PurchaseOfferGrantItemMapper grantItemMapper;
    private final IPurchaseOfferService purchaseOfferService;
    private final IWalletCoreService walletCoreService;
    private final IPurchaseReversalService purchaseReversalService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder applyEvent(PurchasePaymentCallbackBo bo) {
        String tenantId = StringUtils.blankToDefault(bo.getTenantId(), DEFAULT_TENANT_ID);
        String requestHash = requestHash(bo);
        PurchasePaymentEvent existingEvent = eventMapper.selectByEventKey(tenantId, bo.getEventKey());
        if (existingEvent != null) {
            if (!requestHash.equals(existingEvent.getRequestHash())) {
                throw new ServiceException(MessageUtils.message("payment.purchase.event.idempotency.conflict"));
            }
            PurchaseOrder order = orderMapper.selectByOrderNoForUpdate(tenantId, existingEvent.getPurchaseOrderNo());
            if (order == null) {
                throw new ServiceException(MessageUtils.message("payment.purchase.order.not.exists"));
            }
            return order;
        }

        Date now = new Date();
        PurchasePaymentEvent event = buildEvent(tenantId, bo, requestHash, now);
        eventMapper.insert(event);
        try {
            PurchaseOrder order = orderMapper.selectByOrderNoForUpdate(tenantId, bo.getPurchaseOrderNo());
            if (order == null) {
                throw new ServiceException(MessageUtils.message("payment.purchase.order.not.exists"));
            }
            EventProcessingResult processing = processEvent(order, bo, now);
            event.setEventStatus(PurchasePaymentEventStatus.PROCESSED.name());
            event.setProcessResult(processing.processResult());
            event.setProcessTime(now);
            eventMapper.updateById(event);
            return processing.order();
        } catch (RuntimeException ex) {
            event.setEventStatus(PurchasePaymentEventStatus.FAILED.name());
            event.setProcessResult(StringUtils.substring(ex.getMessage(), 0, 500));
            event.setProcessTime(now);
            eventMapper.updateById(event);
            throw ex;
        }
    }

    private EventProcessingResult processEvent(PurchaseOrder order, PurchasePaymentCallbackBo bo, Date now) {
        PurchasePaymentEventType type = bo.getEventType();
        if (PurchasePaymentEventType.PAY_SUCCESS.equals(type)) {
            return ok(processPaySuccess(order, bo, now));
        }
        if (PurchasePaymentEventType.PAY_FAILED.equals(type)) {
            requireStatus(order, PurchaseOrderStatus.CREATED, PurchaseOrderStatus.PENDING);
            order.setStatus(PurchaseOrderStatus.FAILED.name());
            order.setFailReason(StringUtils.substring(StringUtils.blankToDefault(bo.getFailReason(), "Payment failed"), 0, 500));
            order.setCallbackEventKey(bo.getEventKey());
            orderMapper.updateById(order);
            return ok(order);
        }
        if (PurchasePaymentEventType.CANCELLED.equals(type)) {
            requireStatus(order, PurchaseOrderStatus.CREATED, PurchaseOrderStatus.PENDING);
            order.setStatus(PurchaseOrderStatus.CANCELLED.name());
            order.setCancelTime(now);
            order.setCallbackEventKey(bo.getEventKey());
            orderMapper.updateById(order);
            return ok(order);
        }
        if (PurchasePaymentEventType.REFUNDED.equals(type) || PurchasePaymentEventType.CHARGEBACK.equals(type)) {
            PurchaseReversalResult reversal = purchaseReversalService.reverse(order, bo, now);
            return new EventProcessingResult(reversal.getOrder(), reversal.getProcessResult());
        }
        throw new ServiceException(MessageUtils.message("payment.purchase.event.type.unsupported"));
    }

    private EventProcessingResult ok(PurchaseOrder order) {
        return new EventProcessingResult(order, "OK");
    }

    private record EventProcessingResult(PurchaseOrder order, String processResult) {
    }

    private PurchaseOrder processPaySuccess(PurchaseOrder order, PurchasePaymentCallbackBo bo, Date now) {
        requireStatus(order, PurchaseOrderStatus.CREATED, PurchaseOrderStatus.PENDING, PurchaseOrderStatus.PAID, PurchaseOrderStatus.CREDITED);
        if (PurchaseOrderStatus.CREDITED.name().equals(order.getStatus())) {
            return order;
        }
        order.setStatus(PurchaseOrderStatus.PAID.name());
        if (order.getPaidTime() == null) {
            order.setPaidTime(now);
        }
        order.setCallbackEventKey(bo.getEventKey());
        orderMapper.updateById(order);

        List<WalletCreditBo> credits = purchaseOfferService.creditsFromOrderSnapshots(order);
        for (WalletCreditBo credit : credits) {
            WalletTransaction tx = walletCoreService.credit(credit);
            if (!WalletTransactionStatus.SUCCESS.name().equals(tx.getStatus())) {
                throw new ServiceException(StringUtils.blankToDefault(tx.getFailReason(), MessageUtils.message("client.purchase.credit.failed")));
            }
        }
        order.setStatus(PurchaseOrderStatus.CREDITED.name());
        order.setCreditedTime(now);
        order.setFailReason(null);
        orderMapper.updateById(order);
        return order;
    }

    private void requireStatus(PurchaseOrder order, PurchaseOrderStatus... allowed) {
        for (PurchaseOrderStatus status : allowed) {
            if (status.name().equals(order.getStatus())) {
                return;
            }
        }
        throw new ServiceException(MessageUtils.message("payment.purchase.order.status.invalid"));
    }

    private PurchasePaymentEvent buildEvent(String tenantId, PurchasePaymentCallbackBo bo, String requestHash, Date now) {
        PurchasePaymentEvent event = new PurchasePaymentEvent();
        event.setId(IdUtil.getSnowflakeNextId());
        event.setTenantId(tenantId);
        event.setEventKey(bo.getEventKey());
        event.setPurchaseOrderNo(bo.getPurchaseOrderNo());
        event.setProviderCode(StringUtils.blankToDefault(bo.getProviderCode(), DEFAULT_PROVIDER));
        event.setProviderOrderNo(bo.getProviderOrderNo());
        event.setEventType(bo.getEventType().name());
        event.setEventStatus(PurchasePaymentEventStatus.RECEIVED.name());
        event.setRequestHash(requestHash);
        event.setRequestBody(bo.getRequestBody());
        event.setCreateTime(now);
        return event;
    }

    private String requestHash(PurchasePaymentCallbackBo bo) {
        String normalized = StringUtils.blankToDefault(bo.getEventType() == null ? null : bo.getEventType().name(), "")
            + "|" + StringUtils.blankToDefault(bo.getPurchaseOrderNo(), "")
            + "|" + StringUtils.blankToDefault(bo.getProviderCode(), DEFAULT_PROVIDER)
            + "|" + StringUtils.blankToDefault(bo.getProviderOrderNo(), "")
            + "|" + StringUtils.blankToDefault(bo.getRequestBody(), "")
            + "|" + StringUtils.blankToDefault(bo.getFailReason(), "");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
