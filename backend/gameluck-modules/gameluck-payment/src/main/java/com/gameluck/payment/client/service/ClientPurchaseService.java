package com.gameluck.payment.client.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.client.domain.bo.ClientPurchasePayBo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseGrantItemVo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseOfferVo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseOrderVo;
import com.gameluck.payment.domain.PurchaseOffer;
import com.gameluck.payment.domain.PurchaseOfferGrantItem;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.mapper.PurchaseOfferGrantItemMapper;
import com.gameluck.payment.mapper.PurchaseOfferMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.service.IPurchaseOfferService;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * C-side purchase service.
 */
@RequiredArgsConstructor
@Service
public class ClientPurchaseService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String ENABLED = "0";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_CREDITED = "CREDITED";
    private static final String STATUS_FAILED = "FAILED";
    private static final int MONEY_SCALE = 6;

    private final ClientTokenService clientTokenService;
    private final PurchaseOfferMapper offerMapper;
    private final PurchaseOfferGrantItemMapper grantItemMapper;
    private final PurchaseOrderMapper orderMapper;
    private final IPurchaseOfferService purchaseOfferService;
    private final IWalletCoreService walletCoreService;

    public List<ClientPurchaseOfferVo> offers() {
        String tenantId = currentTenantId();
        Date now = new Date();
        List<PurchaseOffer> offers = offerMapper.selectList(Wrappers.<PurchaseOffer>lambdaQuery()
            .eq(PurchaseOffer::getTenantId, tenantId)
            .eq(PurchaseOffer::getStatus, ENABLED)
            .and(q -> q.isNull(PurchaseOffer::getStartTime).or().le(PurchaseOffer::getStartTime, now))
            .and(q -> q.isNull(PurchaseOffer::getEndTime).or().ge(PurchaseOffer::getEndTime, now))
            .orderByAsc(PurchaseOffer::getSortOrder)
            .orderByDesc(PurchaseOffer::getCreateTime));
        if (offers.isEmpty()) {
            return List.of();
        }
        List<Long> offerIds = offers.stream().map(PurchaseOffer::getId).toList();
        Map<Long, List<PurchaseOfferGrantItem>> grouped = grantItemMapper.selectList(Wrappers.<PurchaseOfferGrantItem>lambdaQuery()
                .in(PurchaseOfferGrantItem::getOfferId, offerIds)
                .orderByAsc(PurchaseOfferGrantItem::getSortOrder))
            .stream().collect(Collectors.groupingBy(PurchaseOfferGrantItem::getOfferId));
        return offers.stream()
            .map(offer -> toOfferVo(offer, grouped.getOrDefault(offer.getId(), List.of())))
            .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ClientPurchaseOrderVo pay(String authorization, ClientPurchasePayBo bo) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        String tenantId = currentTenantId();
        PurchaseOrder exists = orderMapper.selectByIdempotencyKey(tenantId, bo.getIdempotencyKey());
        if (exists != null) {
            if (!memberId.equals(exists.getMemberId()) || !bo.getOfferId().equals(exists.getOfferId())) {
                throw new ServiceException(MessageUtils.message("wallet.idempotency.conflict"));
            }
            return toOrderVo(exists, List.of(), null);
        }
        PurchaseOffer offer = requireAvailableOffer(tenantId, bo.getOfferId());
        List<PurchaseOfferGrantItem> items = grantItemMapper.selectList(Wrappers.<PurchaseOfferGrantItem>lambdaQuery()
            .eq(PurchaseOfferGrantItem::getTenantId, tenantId)
            .eq(PurchaseOfferGrantItem::getOfferId, offer.getId())
            .orderByAsc(PurchaseOfferGrantItem::getSortOrder));
        if (items.isEmpty()) {
            throw new ServiceException(MessageUtils.message("payment.purchase.order.grant.required"));
        }
        Date now = new Date();
        PurchaseOrder order = buildOrder(tenantId, memberId, offer, bo.getIdempotencyKey(), now);
        orderMapper.insert(order);
        order.setStatus(STATUS_PAID);
        order.setPaidTime(now);
        orderMapper.updateById(order);
        try {
            List<WalletCreditBo> credits = purchaseOfferService.snapshotPaidOrderGrants(order, items);
            for (WalletCreditBo credit : credits) {
                WalletTransaction tx = walletCoreService.credit(credit);
                if (!WalletTransactionStatus.SUCCESS.name().equals(tx.getStatus())) {
                    throw new ServiceException(StringUtils.blankToDefault(tx.getFailReason(), MessageUtils.message("client.purchase.credit.failed")));
                }
            }
            order.setStatus(STATUS_CREDITED);
            order.setCreditedTime(new Date());
            orderMapper.updateById(order);
            return toOrderVo(order, items, offer);
        } catch (RuntimeException ex) {
            order.setStatus(STATUS_FAILED);
            order.setFailReason(ex.getMessage());
            orderMapper.updateById(order);
            throw ex;
        }
    }

    private PurchaseOffer requireAvailableOffer(String tenantId, Long offerId) {
        PurchaseOffer offer = offerMapper.selectById(offerId);
        Date now = new Date();
        if (offer == null || !tenantId.equals(offer.getTenantId()) || !ENABLED.equals(offer.getStatus())
            || (offer.getStartTime() != null && offer.getStartTime().after(now))
            || (offer.getEndTime() != null && offer.getEndTime().before(now))) {
            throw new ServiceException(MessageUtils.message("client.purchase.offer.not.available"));
        }
        return offer;
    }

    private PurchaseOrder buildOrder(String tenantId, Long memberId, PurchaseOffer offer, String idempotencyKey, Date now) {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(IdUtil.getSnowflakeNextId());
        order.setTenantId(tenantId);
        order.setPurchaseOrderNo("PO" + IdUtil.getSnowflakeNextIdStr());
        order.setOfferId(offer.getId());
        order.setOfferNo(offer.getOfferNo());
        order.setMemberId(memberId);
        order.setPayCurrencyCode(offer.getPayCurrencyCode());
        order.setPayAmount(offer.getPayAmount());
        order.setStatus(STATUS_PENDING);
        order.setIdempotencyKey(idempotencyKey);
        return order;
    }

    private ClientPurchaseOfferVo toOfferVo(PurchaseOffer offer, List<PurchaseOfferGrantItem> items) {
        ClientPurchaseOfferVo vo = new ClientPurchaseOfferVo();
        vo.setOfferId(offer.getId());
        vo.setOfferNo(offer.getOfferNo());
        vo.setOfferName(offer.getOfferName());
        vo.setOfferType(offer.getOfferType());
        vo.setPayCurrencyCode(offer.getPayCurrencyCode());
        vo.setPayAmount(offer.getPayAmount());
        vo.setGrantItems(items.stream().map(this::toGrantVo).toList());
        vo.setLimitText("NONE".equals(offer.getPurchaseLimitType()) ? "No purchase limit." : offer.getPurchaseLimitType());
        vo.setWageringText(wageringText(items));
        return vo;
    }

    private ClientPurchaseOrderVo toOrderVo(PurchaseOrder order, List<PurchaseOfferGrantItem> items, PurchaseOffer offer) {
        ClientPurchaseOrderVo vo = new ClientPurchaseOrderVo();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getPurchaseOrderNo());
        vo.setOfferId(order.getOfferId());
        vo.setOfferNo(order.getOfferNo());
        vo.setOfferName(offer == null ? null : offer.getOfferName());
        vo.setPayCurrencyCode(order.getPayCurrencyCode());
        vo.setPayAmount(order.getPayAmount());
        vo.setStatus(order.getStatus());
        vo.setGrantItems(items.stream().map(this::toGrantVo).toList());
        vo.setCreditedAt(order.getCreditedTime());
        return vo;
    }

    private ClientPurchaseGrantItemVo toGrantVo(PurchaseOfferGrantItem item) {
        BigDecimal required = requiredTurnover(item);
        ClientPurchaseGrantItemVo vo = new ClientPurchaseGrantItemVo();
        vo.setGrantType(item.getGrantType());
        vo.setCurrencyCode(item.getCurrencyCode());
        vo.setGrantAmount(item.getGrantAmount());
        vo.setWageringMode(item.getWageringMode());
        vo.setRequiredTurnover(required);
        vo.setWageringMultiplier(item.getWageringMultiplier());
        vo.setGameScopeType(item.getGameScopeType());
        vo.setGameScopeValue(item.getGameScopeValue());
        return vo;
    }

    private BigDecimal requiredTurnover(PurchaseOfferGrantItem item) {
        String mode = StringUtils.blankToDefault(item.getWageringMode(), "NONE");
        if ("FIXED".equals(mode)) {
            return normalize(item.getWageringRequiredAmount());
        }
        if ("MULTIPLIER".equals(mode)) {
            BigDecimal multiplier = item.getWageringMultiplier() == null ? BigDecimal.ZERO : item.getWageringMultiplier();
            return normalize(item.getGrantAmount().multiply(multiplier));
        }
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String wageringText(List<PurchaseOfferGrantItem> items) {
        return items.stream()
            .filter(item -> requiredTurnover(item).compareTo(BigDecimal.ZERO) > 0)
            .findFirst()
            .map(item -> item.getCurrencyCode() + " requires " + normalize(item.getWageringMultiplier()).stripTrailingZeros().toPlainString() + "x wagering.")
            .orElse("No wagering required.");
    }

    private BigDecimal normalize(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }
}
