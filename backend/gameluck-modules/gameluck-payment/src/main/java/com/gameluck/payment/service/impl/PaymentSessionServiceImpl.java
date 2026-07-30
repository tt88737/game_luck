package com.gameluck.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.client.domain.bo.ClientPaymentSessionCreateBo;
import com.gameluck.payment.client.domain.vo.ClientPaymentSessionVo;
import com.gameluck.payment.domain.PaymentSession;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.enums.PaymentSessionStatus;
import com.gameluck.payment.enums.PurchaseOrderStatus;
import com.gameluck.payment.mapper.PaymentSessionMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.provider.PaymentProviderAdapter;
import com.gameluck.payment.provider.PaymentProviderRegistry;
import com.gameluck.payment.provider.PaymentProviderSessionRequest;
import com.gameluck.payment.provider.PaymentProviderSessionResult;
import com.gameluck.payment.service.IPaymentSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

@RequiredArgsConstructor
@Service
public class PaymentSessionServiceImpl implements IPaymentSessionService {
    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String DEFAULT_PROVIDER = "SIMULATED";
    private static final int MAX_REQUEST_KEY_LENGTH = 128;
    private static final int MAX_PROVIDER_SESSION_LENGTH = 128;
    private static final int MAX_CHECKOUT_URL_LENGTH = 1000;
    private static final long MAX_PROVIDER_TTL_SECONDS = 2 * 60 * 60;

    private final PaymentSessionMapper sessionMapper;
    private final PurchaseOrderMapper orderMapper;
    private final PaymentProviderRegistry providerRegistry;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientPaymentSessionVo create(Long memberId, String orderNo, ClientPaymentSessionCreateBo bo) {
        String tenantId = currentTenantId();
        String requestKey = bo == null ? null : StringUtils.trim(bo.getRequestKey());
        if (memberId == null || StringUtils.isBlank(orderNo) || orderNo.length() > 64) {
            throw error("payment.purchase.order.not.exists");
        }
        String providerCode = normalizeProvider(bo == null ? null : bo.getProviderCode());
        if (StringUtils.isBlank(requestKey) || requestKey.length() > MAX_REQUEST_KEY_LENGTH) {
            throw error("payment.session.request.key.required");
        }
        if (!DEFAULT_PROVIDER.equals(providerCode)) {
            throw error("payment.session.provider.unsupported");
        }

        PurchaseOrder order = orderMapper.selectByOrderNoForUpdate(tenantId, orderNo);
        if (order == null) {
            throw error("payment.purchase.order.not.exists");
        }
        if (!tenantId.equals(order.getTenantId()) || !memberId.equals(order.getMemberId())) {
            throw error("payment.session.not.exists");
        }
        if (!PurchaseOrderStatus.PENDING.name().equals(order.getStatus())) {
            throw error("payment.purchase.order.status.invalid");
        }

        PaymentSession replay = sessionMapper.selectByRequestKey(tenantId, requestKey);
        if (replay != null) {
            requireSameRequest(replay, tenantId, memberId, orderNo, providerCode);
            return toVo(replay);
        }

        Date now = new Date();
        PaymentSession active = sessionMapper.selectActiveByOrderNoForUpdate(tenantId, orderNo, now);
        if (isActive(active, now)) {
            if (matchesOrderSnapshot(active, order, tenantId, memberId, providerCode)) {
                return toVo(active);
            }
            throw error("payment.session.active.conflict");
        }

        PaymentProviderAdapter adapter = providerRegistry.resolve(providerCode);
        PaymentProviderSessionResult result = adapter.createSession(new PaymentProviderSessionRequest(
            Long.valueOf(tenantId), order.getPurchaseOrderNo(), order.getPayCurrencyCode(), order.getPayAmount(), requestKey));
        validateResult(result, order);

        PaymentSession session = new PaymentSession();
        session.setId(IdUtil.getSnowflakeNextId());
        session.setTenantId(tenantId);
        session.setSessionNo("PS" + IdUtil.getSnowflakeNextIdStr());
        session.setPurchaseOrderId(order.getId());
        session.setPurchaseOrderNo(order.getPurchaseOrderNo());
        session.setMemberId(memberId);
        session.setProviderCode(providerCode);
        session.setProviderSessionNo(result.providerSessionNo());
        session.setPayCurrencyCode(order.getPayCurrencyCode());
        session.setPayAmount(order.getPayAmount());
        session.setCheckoutUrl(result.checkoutUrl());
        session.setStatus(PaymentSessionStatus.PENDING.name());
        session.setRequestKey(requestKey);
        session.setExpireTime(Date.from(result.expireTime()));
        session.setVersion(0);
        session.setCreateTime(now);
        session.setUpdateTime(now);
        try {
            sessionMapper.insert(session);
        } catch (DuplicateKeyException duplicate) {
            PaymentSession winner = sessionMapper.selectByRequestKeyForUpdate(tenantId, requestKey);
            if (winner != null) {
                requireSameRequest(winner, tenantId, memberId, orderNo, providerCode);
                return toVo(winner);
            }
            throw duplicate;
        }

        order.setProviderCode(providerCode);
        order.setProviderOrderNo(result.providerSessionNo());
        order.setPaymentSessionNo(session.getSessionNo());
        order.setUpdateTime(now);
        orderMapper.updateById(order);
        return toVo(session);
    }

    @Override
    public ClientPaymentSessionVo get(Long memberId, String sessionNo) {
        if (memberId == null || StringUtils.isBlank(sessionNo) || sessionNo.length() > 64) {
            throw error("payment.session.not.exists");
        }
        String tenantId = currentTenantId();
        PaymentSession session = sessionMapper.selectBySessionNo(tenantId, sessionNo);
        if (session == null || !tenantId.equals(session.getTenantId()) || !memberId.equals(session.getMemberId())) {
            throw error("payment.session.not.exists");
        }
        return toVo(session);
    }

    private void requireSameRequest(PaymentSession session, String tenantId, Long memberId,
                                    String orderNo, String providerCode) {
        if (!tenantId.equals(session.getTenantId()) || !memberId.equals(session.getMemberId())
            || !orderNo.equals(session.getPurchaseOrderNo())
            || !providerCode.equals(session.getProviderCode())) {
            throw error("payment.session.request.conflict");
        }
    }

    private void validateResult(PaymentProviderSessionResult result, PurchaseOrder order) {
        Instant now = Instant.now();
        if (result == null || StringUtils.isBlank(result.providerSessionNo())
            || result.providerSessionNo().length() > MAX_PROVIDER_SESSION_LENGTH
            || !validCheckoutUrl(result.checkoutUrl())
            || result.expireTime() == null || !result.expireTime().isAfter(now)
            || result.expireTime().isAfter(now.plusSeconds(MAX_PROVIDER_TTL_SECONDS))
            || !order.getPurchaseOrderNo().equals(result.purchaseOrderNo())
            || !order.getPayCurrencyCode().equals(result.payCurrencyCode())
            || result.payAmount() == null || order.getPayAmount().compareTo(result.payAmount()) != 0) {
            throw error("payment.session.provider.result.invalid");
        }
    }

    private boolean validCheckoutUrl(String value) {
        if (StringUtils.isBlank(value) || value.length() > MAX_CHECKOUT_URL_LENGTH) return false;
        try {
            URI uri = URI.create(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                && StringUtils.isNotBlank(uri.getHost()) && uri.getUserInfo() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isActive(PaymentSession session, Date now) {
        return session != null
            && (PaymentSessionStatus.CREATED.name().equals(session.getStatus())
                || PaymentSessionStatus.PENDING.name().equals(session.getStatus()))
            && session.getExpireTime() != null && session.getExpireTime().after(now);
    }

    private boolean matchesOrderSnapshot(PaymentSession session, PurchaseOrder order, String tenantId,
                                         Long memberId, String providerCode) {
        return tenantId.equals(session.getTenantId())
            && order.getId().equals(session.getPurchaseOrderId())
            && order.getPurchaseOrderNo().equals(session.getPurchaseOrderNo())
            && memberId.equals(session.getMemberId())
            && providerCode.equals(session.getProviderCode())
            && order.getPayCurrencyCode().equals(session.getPayCurrencyCode())
            && session.getPayAmount() != null && order.getPayAmount().compareTo(session.getPayAmount()) == 0;
    }

    private String normalizeProvider(String code) {
        String trimmed = StringUtils.blankToDefault(StringUtils.trim(code), DEFAULT_PROVIDER);
        if (trimmed.length() > 32) throw error("payment.session.provider.unsupported");
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private ClientPaymentSessionVo toVo(PaymentSession session) {
        ClientPaymentSessionVo vo = new ClientPaymentSessionVo();
        vo.setSessionNo(session.getSessionNo()); vo.setOrderNo(session.getPurchaseOrderNo());
        vo.setProviderCode(session.getProviderCode()); vo.setProviderSessionNo(session.getProviderSessionNo());
        vo.setPayCurrencyCode(session.getPayCurrencyCode()); vo.setPayAmount(session.getPayAmount());
        vo.setCheckoutUrl(session.getCheckoutUrl()); vo.setStatus(session.getStatus());
        vo.setExpireTime(session.getExpireTime()); vo.setCompletedTime(session.getCompletedTime());
        return vo;
    }

    private ServiceException error(String key) { return new ServiceException(MessageUtils.message(key)); }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }
}
