package com.gameluck.payment.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.client.domain.bo.ClientPaymentSessionCreateBo;
import com.gameluck.payment.client.domain.vo.ClientPaymentSessionVo;
import com.gameluck.payment.domain.PaymentSession;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.mapper.PaymentSessionMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.provider.PaymentProviderAdapter;
import com.gameluck.payment.provider.PaymentProviderRegistry;
import com.gameluck.payment.provider.PaymentProviderSessionRequest;
import com.gameluck.payment.provider.PaymentProviderSessionResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("local")
class PaymentSessionServiceImplTest {

    @Test
    void sameRequestKeyReturnsSameSessionWithoutCallingProvider() {
        Fixture f = fixture();
        when(f.orders.selectByOrderNoForUpdate("000000", "PO1")).thenReturn(order("PO1", 1001L, "PENDING"));
        PaymentSession existing = session("PS1", "PO1", 1001L, "SIMULATED", "PENDING", future());
        when(f.sessions.selectByRequestKey("000000", "rk-1")).thenReturn(existing);

        ClientPaymentSessionVo result = f.service.create(1001L, "PO1", bo("rk-1", "SIMULATED"));

        assertEquals("PS1", result.getSessionNo());
        verifyNoInteractions(f.registry);
        verify(f.sessions, never()).insert(any(PaymentSession.class));
        verify(f.orders, never()).updateById(any(PurchaseOrder.class));
    }

    @Test
    void sameRequestKeyDoesNotReplayWhenLockedOrderIsTerminal() {
        for (String status : new String[]{"CREDITED", "REFUNDED", "CHARGEBACK", "FAILED", "CANCELLED"}) {
            Fixture f = fixture();
            when(f.orders.selectByOrderNoForUpdate("000000", "PO1")).thenReturn(order("PO1", 1001L, status));
            when(f.sessions.selectByRequestKey("000000", "rk-1"))
                .thenReturn(session("PS1", "PO1", 1001L, "SIMULATED", "PENDING", future()));

            assertThrows(ServiceException.class, () -> f.service.create(1001L, "PO1", bo("rk-1", "SIMULATED")));
            verify(f.sessions, never()).selectByRequestKey(anyString(), anyString());
            verifyNoInteractions(f.registry);
        }
    }

    @Test
    void sameRequestKeyWithDifferentOrderMemberOrProviderConflicts() {
        Fixture f = fixture();
        when(f.orders.selectByOrderNoForUpdate("000000", "PO1")).thenReturn(order("PO1", 1001L, "PENDING"));
        when(f.orders.selectByOrderNoForUpdate("000000", "PO2")).thenReturn(order("PO2", 1001L, "PENDING"));
        when(f.sessions.selectByRequestKey("000000", "rk-1"))
            .thenReturn(session("PS1", "PO1", 1001L, "SIMULATED", "PENDING", future()));

        assertThrows(ServiceException.class, () -> f.service.create(1001L, "PO2", bo("rk-1", "SIMULATED")));
        assertThrows(ServiceException.class, () -> f.service.create(1002L, "PO1", bo("rk-1", "SIMULATED")));
        assertThrows(ServiceException.class, () -> f.service.create(1001L, "PO1", bo("rk-1", "OTHER")));
    }

    @Test
    void activeSessionWithMismatchedIdentityOrSnapshotConflicts() {
        for (String mismatch : new String[]{"orderNo", "orderId", "amount", "currency"}) {
            Fixture f = fixture();
            PurchaseOrder order = order("PO1", 1001L, "PENDING");
            when(f.orders.selectByOrderNoForUpdate("000000", "PO1")).thenReturn(order);
            PaymentSession active = session("PS1", "PO1", 1001L, "SIMULATED", "PENDING", future());
            if ("orderNo".equals(mismatch)) active.setPurchaseOrderNo("PO-X");
            if ("orderId".equals(mismatch)) active.setPurchaseOrderId(99L);
            if ("amount".equals(mismatch)) active.setPayAmount(new BigDecimal("99.00"));
            if ("currency".equals(mismatch)) active.setPayCurrencyCode("EUR");
            when(f.sessions.selectActiveByOrderNoForUpdate(eq("000000"), eq("PO1"), any(Date.class))).thenReturn(active);

            assertThrows(ServiceException.class, () -> f.service.create(1001L, "PO1", bo("rk-2", "SIMULATED")), mismatch);
            verifyNoInteractions(f.registry);
            verify(f.sessions, never()).insert(any(PaymentSession.class));
        }
    }

    @Test
    void onlySimulatedProviderIsAcceptedAfterNormalization() {
        Fixture allowed = fixture();
        when(allowed.orders.selectByOrderNoForUpdate("000000", "PO1")).thenReturn(order("PO1", 1001L, "PENDING"));
        successfulProvider(allowed);
        assertNotNull(allowed.service.create(1001L, "PO1", bo("rk", " simulated ")));
        verify(allowed.registry).resolve("SIMULATED");

        for (String provider : new String[]{"ADYEN", "STRIPE"}) {
            Fixture rejected = fixture();
            when(rejected.orders.selectByOrderNoForUpdate("000000", "PO1")).thenReturn(order("PO1", 1001L, "PENDING"));
            assertThrows(ServiceException.class,
                () -> rejected.service.create(1001L, "PO1", bo("rk", provider)));
            verifyNoInteractions(rejected.registry);
        }
    }

    @Test
    void newKeyReusesMatchingUnexpiredActiveSession() {
        Fixture f = fixture();
        PurchaseOrder order = order("PO1", 1001L, "PENDING");
        when(f.orders.selectByOrderNoForUpdate("000000", "PO1")).thenReturn(order);
        when(f.sessions.selectActiveByOrderNoForUpdate(eq("000000"), eq("PO1"), any(Date.class)))
            .thenReturn(session("PS1", "PO1", 1001L, "SIMULATED", "CREATED", future()));

        assertEquals("PS1", f.service.create(1001L, "PO1", bo("rk-2", "SIMULATED")).getSessionNo());
        verifyNoInteractions(f.registry);
    }

    @Test
    void expiredOrFailedOrCancelledSessionAllowsCreation() {
        for (String status : new String[]{"EXPIRED", "FAILED", "CANCELLED"}) {
            Fixture f = fixture();
            when(f.orders.selectByOrderNoForUpdate("000000", "PO1")).thenReturn(order("PO1", 1001L, "PENDING"));
            when(f.sessions.selectActiveByOrderNoForUpdate(eq("000000"), eq("PO1"), any(Date.class)))
                .thenReturn("EXPIRED".equals(status) ? session("OLD", "PO1", 1001L, "SIMULATED", status, past()) : null);
            successfulProvider(f);

            assertNotEquals("OLD", f.service.create(1001L, "PO1", bo("rk-" + status, "SIMULATED")).getSessionNo());
            verify(f.sessions).insert(any(PaymentSession.class));
        }
    }

    @Test
    void nonPendingOrderAndMismatchedOwnerAreRejected() {
        for (String status : new String[]{"CREDITED", "REFUNDED", "CHARGEBACK", "FAILED"}) {
            Fixture f = fixture();
            when(f.orders.selectByOrderNoForUpdate("000000", "PO1")).thenReturn(order("PO1", 1001L, status));
            assertThrows(ServiceException.class, () -> f.service.create(1001L, "PO1", bo("rk", "SIMULATED")));
        }
        Fixture f = fixture();
        when(f.orders.selectByOrderNoForUpdate("000000", "PO1")).thenReturn(order("PO1", 2002L, "PENDING"));
        assertThrows(ServiceException.class, () -> f.service.create(1001L, "PO1", bo("rk", "SIMULATED")));
    }

    @Test
    void providerFailureLeavesSessionAndOrderUntouched() {
        Fixture f = fixture();
        when(f.orders.selectByOrderNoForUpdate("000000", "PO1")).thenReturn(order("PO1", 1001L, "PENDING"));
        PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class);
        when(f.registry.resolve("SIMULATED")).thenReturn(adapter);
        when(adapter.createSession(any())).thenThrow(new IllegalStateException("provider down"));

        assertThrows(IllegalStateException.class, () -> f.service.create(1001L, "PO1", bo("rk", "SIMULATED")));
        verify(f.sessions, never()).insert(any(PaymentSession.class));
        verify(f.orders, never()).updateById(any(PurchaseOrder.class));
    }

    @Test
    void duplicateRequestKeyReturnsConcurrentWinnerOrConflicts() {
        Fixture f = fixture();
        when(f.orders.selectByOrderNoForUpdate("000000", "PO1")).thenReturn(order("PO1", 1001L, "PENDING"));
        successfulProvider(f);
        when(f.sessions.insert(any(PaymentSession.class))).thenThrow(new DuplicateKeyException("race"));
        when(f.sessions.selectByRequestKey("000000", "rk")).thenReturn(null);
        when(f.sessions.selectByRequestKeyForUpdate("000000", "rk"))
            .thenReturn(session("WINNER", "PO1", 1001L, "SIMULATED", "PENDING", future()));
        assertEquals("WINNER", f.service.create(1001L, "PO1", bo("rk", "SIMULATED")).getSessionNo());
        verify(f.orders, never()).updateById(any(PurchaseOrder.class));
        verify(f.sessions).selectByRequestKeyForUpdate("000000", "rk");
    }

    @Test
    void invalidProviderResultsNeverWrite() {
        Object[][] bad = {
            {null, "https://pay.test/1", Instant.now().plusSeconds(600), "USD", new BigDecimal("12.34")},
            {"X".repeat(129), "https://pay.test/1", Instant.now().plusSeconds(600), "USD", new BigDecimal("12.34")},
            {"P1", "javascript:alert(1)", Instant.now().plusSeconds(600), "USD", new BigDecimal("12.34")},
            {"P1", "/relative", Instant.now().plusSeconds(600), "USD", new BigDecimal("12.34")},
            {"P1", "https://user@pay.test/1", Instant.now().plusSeconds(600), "USD", new BigDecimal("12.34")},
            {"P1", "https://pay.test/1", Instant.now().plusSeconds(600), "USD", null},
            {"P1", "https://pay.test/1", Instant.now().plusSeconds(60 * 60 * 24), "USD", new BigDecimal("12.34")}
        };
        for (Object[] row : bad) {
            Fixture f = fixture();
            when(f.orders.selectByOrderNoForUpdate("000000", "PO1")).thenReturn(order("PO1", 1001L, "PENDING"));
            PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class);
            when(f.registry.resolve("SIMULATED")).thenReturn(adapter);
            when(adapter.createSession(any())).thenReturn(new PaymentProviderSessionResult(
                (String) row[0], (String) row[1], (Instant) row[2], "PO1", (String) row[3], (BigDecimal) row[4]));
            assertThrows(ServiceException.class, () -> f.service.create(1001L, "PO1", bo("rk", "SIMULATED")));
            verify(f.sessions, never()).insert(any(PaymentSession.class));
            verify(f.orders, never()).updateById(any(PurchaseOrder.class));
        }
    }

    @Test
    void oversizedRequestKeyIsRejectedBeforeMapperOrProvider() {
        Fixture f = fixture();
        assertThrows(ServiceException.class, () -> f.service.create(1001L, "PO1", bo("x".repeat(129), "SIMULATED")));
        verifyNoInteractions(f.sessions, f.orders, f.registry);
    }

    @Test
    void createsSessionFromLockedOrderSnapshotAndUpdatesOrderAfterProviderSuccess() {
        Fixture f = fixture();
        PurchaseOrder order = order("PO1", 1001L, "PENDING");
        when(f.orders.selectByOrderNoForUpdate("000000", "PO1")).thenReturn(order);
        successfulProvider(f);

        ClientPaymentSessionVo result = f.service.create(1001L, "PO1", bo("rk", null));

        ArgumentCaptor<PaymentProviderSessionRequest> request = ArgumentCaptor.forClass(PaymentProviderSessionRequest.class);
        verify(f.adapter).createSession(request.capture());
        assertEquals(0, new BigDecimal("12.340000").compareTo(request.getValue().payAmount()));
        assertEquals("USD", request.getValue().payCurrencyCode());
        assertEquals("PO1", request.getValue().purchaseOrderNo());
        verify(f.sessions).insert(any(PaymentSession.class));
        verify(f.orders).updateById(order);
        assertEquals("SIMULATED", order.getProviderCode());
        assertEquals("PROVIDER-1", order.getProviderOrderNo());
        assertEquals(result.getSessionNo(), order.getPaymentSessionNo());
    }

    @Test
    void getRequiresTenantAndMemberOwnership() {
        Fixture f = fixture();
        when(f.sessions.selectBySessionNo("000000", "PS1"))
            .thenReturn(session("PS1", "PO1", 1001L, "SIMULATED", "PENDING", future()));
        assertEquals("PS1", f.service.get(1001L, "PS1").getSessionNo());
        assertThrows(ServiceException.class, () -> f.service.get(2002L, "PS1"));
        verify(f.sessions, times(2)).selectBySessionNo("000000", "PS1");
    }

    @Test
    void createAndGetUseCurrentNonDefaultTenant() {
        Fixture f = fixture();
        PurchaseOrder order = order("PO1", 1001L, "PENDING");
        order.setTenantId("100001");
        PaymentSession session = session("PS1", "PO1", 1001L, "SIMULATED", "PENDING", future());
        session.setTenantId("100001");
        when(f.orders.selectByOrderNoForUpdate("100001", "PO1")).thenReturn(order);
        when(f.sessions.selectByRequestKey("100001", "rk")).thenReturn(session);
        when(f.sessions.selectBySessionNo("100001", "PS1")).thenReturn(session);
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("100001");
            assertEquals("PS1", f.service.create(1001L, "PO1", bo("rk", "SIMULATED")).getSessionNo());
            assertEquals("PS1", f.service.get(1001L, "PS1").getSessionNo());
        }
        verify(f.orders).selectByOrderNoForUpdate("100001", "PO1");
        verify(f.sessions).selectBySessionNo("100001", "PS1");
        verify(f.orders, never()).selectByOrderNoForUpdate(eq("000000"), anyString());
    }

    private Fixture fixture() {
        PaymentSessionMapper sessions = mock(PaymentSessionMapper.class);
        PurchaseOrderMapper orders = mock(PurchaseOrderMapper.class);
        PaymentProviderRegistry registry = mock(PaymentProviderRegistry.class);
        return new Fixture(sessions, orders, registry, null,
            new PaymentSessionServiceImpl(sessions, orders, registry));
    }

    private void successfulProvider(Fixture f) {
        PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class);
        f.adapter = adapter;
        when(f.registry.resolve("SIMULATED")).thenReturn(adapter);
        when(adapter.createSession(any())).thenReturn(new PaymentProviderSessionResult(
            "PROVIDER-1", "https://pay.test/1", Instant.now().plusSeconds(600),
            "PO1", "USD", new BigDecimal("12.340000")));
    }

    private ClientPaymentSessionCreateBo bo(String key, String provider) {
        ClientPaymentSessionCreateBo bo = new ClientPaymentSessionCreateBo();
        bo.setRequestKey(key);
        bo.setProviderCode(provider);
        return bo;
    }

    private PurchaseOrder order(String no, Long memberId, String status) {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(11L); order.setTenantId("000000"); order.setPurchaseOrderNo(no);
        order.setMemberId(memberId); order.setStatus(status); order.setPayCurrencyCode("USD");
        order.setPayAmount(new BigDecimal("12.340000"));
        return order;
    }

    private PaymentSession session(String no, String orderNo, Long member, String provider, String status, Date expiry) {
        PaymentSession s = new PaymentSession();
        s.setSessionNo(no); s.setPurchaseOrderNo(orderNo); s.setMemberId(member);
        s.setProviderCode(provider); s.setStatus(status); s.setExpireTime(expiry);
        s.setTenantId("000000"); s.setPurchaseOrderId(11L); s.setPayCurrencyCode("USD");
        s.setPayAmount(new BigDecimal("12.340000"));
        return s;
    }

    private Date future() { return Date.from(Instant.now().plusSeconds(600)); }
    private Date past() { return Date.from(Instant.now().minusSeconds(60)); }

    private static class Fixture {
        final PaymentSessionMapper sessions;
        final PurchaseOrderMapper orders;
        final PaymentProviderRegistry registry;
        PaymentProviderAdapter adapter;
        final PaymentSessionServiceImpl service;
        Fixture(PaymentSessionMapper sessions, PurchaseOrderMapper orders, PaymentProviderRegistry registry,
                PaymentProviderAdapter adapter, PaymentSessionServiceImpl service) {
            this.sessions = sessions; this.orders = orders; this.registry = registry;
            this.adapter = adapter; this.service = service;
        }
    }
}
