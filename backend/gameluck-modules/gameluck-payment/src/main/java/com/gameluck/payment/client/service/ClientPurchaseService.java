package com.gameluck.payment.client.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.member.compliance.MemberComplianceAction;
import com.gameluck.member.compliance.MemberComplianceContext;
import com.gameluck.member.compliance.MemberComplianceDecision;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.mapper.MemberProfileMapper;
import com.gameluck.member.service.IMemberComplianceGateService;
import com.gameluck.payment.client.domain.bo.ClientPurchasePayBo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseGrantItemVo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseOfferVo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseOrderVo;
import com.gameluck.payment.domain.PurchaseOffer;
import com.gameluck.payment.domain.PurchaseOfferGrantItem;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.domain.PurchaseOrderGrantSnapshot;
import com.gameluck.payment.enums.PurchaseOrderStatus;
import com.gameluck.payment.mapper.PurchaseOfferGrantItemMapper;
import com.gameluck.payment.mapper.PurchaseOfferMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.service.IPurchaseOfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

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
    private static final int MONEY_SCALE = 6;

    private final ClientTokenService clientTokenService;
    private final PurchaseOfferMapper offerMapper;
    private final PurchaseOfferGrantItemMapper grantItemMapper;
    private final PurchaseOrderMapper orderMapper;
    private final IPurchaseOfferService purchaseOfferService;
    private final MemberProfileMapper memberProfileMapper;
    private final IMemberComplianceGateService complianceGateService;

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
            return replayVo(exists);
        }
        PurchaseOffer offer = requireAvailableOffer(tenantId, bo.getOfferId());
        validatePurchaseCompliance(tenantId, memberId, offer);
        enforcePurchaseLimit(tenantId, memberId, offer);
        List<PurchaseOfferGrantItem> items = grantItemMapper.selectList(Wrappers.<PurchaseOfferGrantItem>lambdaQuery()
            .eq(PurchaseOfferGrantItem::getTenantId, tenantId)
            .eq(PurchaseOfferGrantItem::getOfferId, offer.getId())
            .orderByAsc(PurchaseOfferGrantItem::getSortOrder));
        if (items.isEmpty()) {
            throw new ServiceException(MessageUtils.message("payment.purchase.order.grant.required"));
        }
        Date now = new Date();
        PurchaseOrder order = buildOrder(tenantId, memberId, offer, bo.getIdempotencyKey(), now);
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException duplicate) {
            PurchaseOrder winner = orderMapper.selectByIdempotencyKeyForUpdate(tenantId, bo.getIdempotencyKey());
            if (winner == null) throw duplicate;
            if (!memberId.equals(winner.getMemberId()) || !bo.getOfferId().equals(winner.getOfferId())) {
                throw new ServiceException(MessageUtils.message("wallet.idempotency.conflict"));
            }
            return reconcileWinnerVo(winner);
        }
        List<PurchaseOrderGrantSnapshot> snapshots = purchaseOfferService.prepareOrderGrantSnapshots(order, items);
        return toOrderVoFromSnapshots(order, snapshots);
    }

    private ClientPurchaseOrderVo replayVo(PurchaseOrder order) {
        return toOrderVoFromSnapshots(order, purchaseOfferService.orderGrantSnapshots(order));
    }

    private ClientPurchaseOrderVo reconcileWinnerVo(PurchaseOrder order) {
        return toOrderVoFromSnapshots(order, purchaseOfferService.orderGrantSnapshotsForUpdate(order));
    }

    private void validatePurchaseCompliance(String tenantId, Long memberId, PurchaseOffer offer) {
        MemberProfile member = memberProfileMapper.selectById(memberId);
        MemberComplianceDecision decision = complianceGateService.evaluate(MemberComplianceContext.builder()
            .tenantId(tenantId)
            .member(member)
            .action(MemberComplianceAction.PURCHASE_PAY)
            .currencyCode(offer.getPayCurrencyCode())
            .channel("h5")
            .build());
        if (!decision.isAllowed()) {
            throw new ServiceException(MessageUtils.message(decision.getMessageKey()));
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

    private void enforcePurchaseLimit(String tenantId, Long memberId, PurchaseOffer offer) {
        String limitType = StringUtils.blankToDefault(offer.getPurchaseLimitType(), "NONE");
        if ("NONE".equals(limitType)) {
            return;
        }
        if ("FIRST_ONLY".equals(limitType)) {
            if (orderMapper.countCreditedByMember(tenantId, memberId) > 0) {
                throw new ServiceException(MessageUtils.message("client.purchase.limit.first.only"));
            }
            return;
        }
        if ("TOTAL_ONCE".equals(limitType)) {
            if (orderMapper.countCreditedByMemberAndOffer(tenantId, memberId, offer.getId()) > 0) {
                throw new ServiceException(MessageUtils.message("client.purchase.limit.total.once"));
            }
            return;
        }
        if ("DAILY_ONCE".equals(limitType)) {
            Date dayStart = DateUtil.beginOfDay(new Date());
            Date nextDayStart = DateUtil.offsetDay(dayStart, 1);
            if (orderMapper.countCreditedByMemberOfferAndCreditedTimeRange(tenantId, memberId, offer.getId(), dayStart, nextDayStart) > 0) {
                throw new ServiceException(MessageUtils.message("client.purchase.limit.daily.once"));
            }
            return;
        }
        throw new ServiceException(MessageUtils.message("client.purchase.limit.unsupported"));
    }

    private PurchaseOrder buildOrder(String tenantId, Long memberId, PurchaseOffer offer, String idempotencyKey, Date now) {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(IdUtil.getSnowflakeNextId());
        order.setTenantId(tenantId);
        order.setPurchaseOrderNo("PO" + IdUtil.getSnowflakeNextIdStr());
        order.setOfferId(offer.getId());
        order.setOfferNo(offer.getOfferNo());
        order.setOfferNameSnapshot(offer.getOfferName());
        order.setMemberId(memberId);
        order.setPayCurrencyCode(offer.getPayCurrencyCode());
        order.setPayAmount(offer.getPayAmount());
        order.setStatus(PurchaseOrderStatus.PENDING.name());
        order.setIdempotencyKey(idempotencyKey);
        order.setCreateTime(now);
        order.setUpdateTime(now);
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
        vo.setProviderCode(order.getProviderCode());
        vo.setProviderOrderNo(order.getProviderOrderNo());
        vo.setPaymentSessionNo(order.getPaymentSessionNo());
        vo.setGrantItems(items.stream().map(this::toGrantVo).toList());
        vo.setCreatedAt(order.getCreateTime());
        vo.setCreditedAt(order.getCreditedTime());
        return vo;
    }

    private ClientPurchaseOrderVo toOrderVoFromSnapshots(PurchaseOrder order, List<PurchaseOrderGrantSnapshot> snapshots) {
        ClientPurchaseOrderVo vo = toOrderVo(order, List.of(), null);
        vo.setOfferName(order.getOfferNameSnapshot());
        vo.setGrantItems(snapshots.stream().map(this::toGrantVo).toList());
        return vo;
    }

    private ClientPurchaseGrantItemVo toGrantVo(PurchaseOrderGrantSnapshot item) {
        ClientPurchaseGrantItemVo vo = new ClientPurchaseGrantItemVo();
        vo.setGrantType(item.getGrantType());
        vo.setCurrencyCode(item.getCurrencyCode());
        vo.setGrantAmount(item.getGrantAmount());
        vo.setWageringMode(item.getWageringMode());
        vo.setRequiredTurnover(item.getRequiredTurnover());
        vo.setWageringMultiplier(item.getWageringMultiplier());
        vo.setGameScopeType(item.getGameScopeType());
        vo.setGameScopeValue(item.getGameScopeValue());
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
