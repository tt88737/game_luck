package com.gameluck.payment.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.payment.domain.PaymentWebhookEvent;
import com.gameluck.payment.mapper.PaymentSessionMapper;
import com.gameluck.payment.mapper.PaymentWebhookEventMapper;
import com.gameluck.payment.domain.bo.PaymentSessionAdminBo;
import com.gameluck.payment.domain.bo.PaymentWebhookEventAdminBo;
import com.gameluck.payment.domain.vo.PaymentSessionAdminVo;
import com.gameluck.payment.domain.vo.PaymentWebhookEventAdminVo;
import com.gameluck.payment.domain.vo.PaymentWebhookEventDetailVo;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.mockito.MockedStatic;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentProviderAdminServiceImplTest {

    @Test
    @Tag("local")
    void retryRejectsEveryStatusExceptFailed() {
        PaymentWebhookEventMapper events = mock(PaymentWebhookEventMapper.class);
        PaymentWebhookBusinessProcessor processor = mock(PaymentWebhookBusinessProcessor.class);
        PaymentWebhookEvent event = event("PROCESSED");
        when(events.selectByIdForUpdate("000001", 7L)).thenReturn(event);
        PaymentProviderAdminServiceImpl service = service(events, processor);

        assertThrows(ServiceException.class, () -> service.retryWebhookEvent("000001", 7L));
        verifyNoInteractions(processor);
    }

    @Test
    @Tag("local")
    void retryUsesSameBusinessProcessorAndDoesNotUpdateImmutablePayload() {
        PaymentWebhookEventMapper events = mock(PaymentWebhookEventMapper.class);
        PaymentWebhookBusinessProcessor processor = mock(PaymentWebhookBusinessProcessor.class);
        PaymentWebhookEvent event = event("FAILED");
        when(events.selectByIdForUpdate("000001", 7L)).thenReturn(event);
        when(processor.processBusiness(7L)).thenReturn(
            new PaymentWebhookBusinessProcessor.WebhookProcessingOutcome("evt-1", "PROCESSED"));
        PaymentWebhookEventDetailVo persisted = new PaymentWebhookEventDetailVo();
        persisted.setId(7L); persisted.setProviderEventId("evt-1"); persisted.setStatus("PROCESSED");
        when(events.selectAdminById("000001", 7L)).thenReturn(persisted);
        PaymentProviderAdminServiceImpl service = service(events, processor);

        var result = service.retryWebhookEvent("000001", 7L);

        assertEquals("PROCESSED", result.getStatus());
        verify(processor).processBusiness(7L);
        verify(events, never()).updateById(any(PaymentWebhookEvent.class));
    }

    @Test @Tag("local")
    void successfulRetryRequiresPersistedTenantDetail() {
        PaymentWebhookEventMapper events = mock(PaymentWebhookEventMapper.class);
        PaymentWebhookBusinessProcessor processor = mock(PaymentWebhookBusinessProcessor.class);
        when(events.selectByIdForUpdate("000001", 7L)).thenReturn(event("FAILED"));
        when(processor.processBusiness(7L)).thenReturn(
            new PaymentWebhookBusinessProcessor.WebhookProcessingOutcome("evt-1", "PROCESSED"));

        ServiceException error = assertThrows(ServiceException.class,
            () -> service(events, processor).retryWebhookEvent("000001", 7L));
        assertNotNull(error.getMessage());
    }

    @Test
    @Tag("local")
    void mapperRetryMetadataSqlCannotMutateRawBodyOrSignatureDigest() throws Exception {
        String sql = PaymentWebhookEventMapper.class.getMethod("recordFailure", String.class, Long.class,
            String.class, Date.class).getAnnotation(org.apache.ibatis.annotations.Update.class).value()[0].toLowerCase();
        assertFalse(sql.contains("raw_body"));
        assertFalse(sql.contains("signature_digest"));
    }

    @Test @Tag("local")
    void mapperAdminSqlIsTenantFirstAndKeepsRawBodyDetailOnly() throws Exception {
        String sessionList = selectSql(PaymentSessionMapper.class, "selectAdminPage");
        assertTrue(sessionList.contains("m.tenant_id=s.tenant_id"));
        assertTrue(sessionList.indexOf("s.tenant_id=#{tenantId}") < sessionList.indexOf("bo.sessionNo"));
        for (String filter : new String[]{"sessionNo", "purchaseOrderNo", "providerSessionNo", "memberId", "memberNo",
            "providerCode", "status", "payCurrencyCode", "beginTime", "endTime"}) assertTrue(sessionList.contains("bo." + filter));
        String eventList = selectSql(PaymentWebhookEventMapper.class, "selectAdminPage");
        assertFalse(eventList.contains("raw_body")); assertFalse(eventList.contains("signature_digest"));
        String eventDetail = selectSql(PaymentWebhookEventMapper.class, "selectAdminById");
        assertTrue(eventDetail.contains("raw_body")); assertTrue(eventDetail.contains("signature_digest"));
        assertTrue(eventDetail.contains("where tenant_id=#{tenantId} and id=#{id}"));
    }

    @Test @Tag("local")
    void retryResultCannotExposeRawPayloadDigestOrSecret() throws Exception {
        Class<?> resultType = Class.forName("com.gameluck.payment.domain.vo.PaymentWebhookRetryResultVo");
        assertTrue(Arrays.stream(resultType.getDeclaredFields()).map(java.lang.reflect.Field::getName)
            .noneMatch(name -> name.toLowerCase().matches(".*(raw|digest|signature|secret|payload).*")));
        assertEquals(resultType, com.gameluck.payment.service.IPaymentProviderAdminService.class
            .getMethod("retryWebhookEvent", Long.class).getReturnType());
    }

    @Test @Tag("local")
    void listAndDetailTypesSeparateSensitiveEvidence() throws Exception {
        Set<String> listFields = Arrays.stream(PaymentWebhookEventAdminVo.class.getDeclaredFields())
            .map(java.lang.reflect.Field::getName).collect(java.util.stream.Collectors.toSet());
        assertFalse(listFields.contains("rawBody")); assertFalse(listFields.contains("signatureDigest"));
        Class<?> detail = Class.forName("com.gameluck.payment.domain.vo.PaymentWebhookEventDetailVo");
        Set<String> detailFields = Arrays.stream(detail.getDeclaredFields())
            .map(java.lang.reflect.Field::getName).collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("rawBody", "signatureDigest"), detailFields);
        assertEquals(detail, com.gameluck.payment.service.IPaymentProviderAdminService.class
            .getMethod("queryWebhookById", Long.class).getReturnType());
    }

    @Test @Tag("local")
    void failedRetryReturnsOnlyConcurrentTerminalSummary() {
        PaymentWebhookEventMapper events = mock(PaymentWebhookEventMapper.class);
        PaymentWebhookBusinessProcessor processor = mock(PaymentWebhookBusinessProcessor.class);
        when(events.selectByIdForUpdate("000001", 7L)).thenReturn(event("FAILED"));
        doThrow(new ServiceException("concurrent")).when(processor).processBusiness(7L);
        PaymentWebhookFailureRecorder recorder = mock(PaymentWebhookFailureRecorder.class);
        PaymentWebhookEvent terminal = event("PROCESSED"); terminal.setProcessingCount(3);
        when(recorder.record("000001", 7L)).thenReturn(terminal);
        PaymentProviderAdminServiceImpl service = new PaymentProviderAdminServiceImpl(
            mock(PaymentSessionMapper.class), events, processor, recorder);

        var result = service.retryWebhookEvent("000001", 7L);
        assertEquals(7L, result.getEventId()); assertEquals("evt-1", result.getProviderEventId());
        assertEquals("PROCESSED", result.getStatus()); assertEquals(3, result.getProcessingCount());
    }

    @Test @Tag("local")
    void pageQueriesPassEveryFilterWithCurrentTenant() {
        PaymentSessionMapper sessions = mock(PaymentSessionMapper.class);
        PaymentWebhookEventMapper events = mock(PaymentWebhookEventMapper.class);
        when(sessions.selectAdminPage(any(), eq("000123"), any())).thenReturn(new Page<PaymentSessionAdminVo>());
        when(events.selectAdminPage(any(), eq("000123"), any())).thenReturn(new Page<PaymentWebhookEventAdminVo>());
        PaymentProviderAdminServiceImpl service = new PaymentProviderAdminServiceImpl(sessions, events,
            mock(PaymentWebhookBusinessProcessor.class), mock(PaymentWebhookFailureRecorder.class));
        PaymentSessionAdminBo sessionBo = new PaymentSessionAdminBo(); sessionBo.setSessionNo("S1");
        PaymentWebhookEventAdminBo eventBo = new PaymentWebhookEventAdminBo(); eventBo.setProviderEventId("E1");
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("000123");
            service.querySessionPage(sessionBo, new PageQuery(10, 1));
            service.queryWebhookPage(eventBo, new PageQuery(10, 1));
        }
        verify(sessions).selectAdminPage(any(), eq("000123"), same(sessionBo));
        verify(events).selectAdminPage(any(), eq("000123"), same(eventBo));
    }

    @Test
    @Tag("local")
    void failedRetryRecordsAnotherMetadataOnlyAttempt() {
        PaymentWebhookEventMapper events = mock(PaymentWebhookEventMapper.class);
        PaymentWebhookBusinessProcessor processor = mock(PaymentWebhookBusinessProcessor.class);
        when(events.selectByIdForUpdate("000001", 7L)).thenReturn(event("FAILED"));
        doThrow(new ServiceException("downstream failed")).when(processor).processBusiness(7L);
        PaymentWebhookFailureRecorder recorder = mock(PaymentWebhookFailureRecorder.class);

        PaymentProviderAdminServiceImpl service = new PaymentProviderAdminServiceImpl(
            mock(PaymentSessionMapper.class), events, processor, recorder);
        assertThrows(ServiceException.class, () -> service.retryWebhookEvent("000001", 7L));

        verify(recorder).record("000001", 7L);
        verify(events, never()).updateById(any(PaymentWebhookEvent.class));
    }

    private PaymentProviderAdminServiceImpl service(PaymentWebhookEventMapper events,
                                                    PaymentWebhookBusinessProcessor processor) {
        return new PaymentProviderAdminServiceImpl(mock(PaymentSessionMapper.class), events, processor,
            mock(PaymentWebhookFailureRecorder.class));
    }

    private PaymentWebhookEvent event(String status) {
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setId(7L);
        event.setTenantId("000001");
        event.setProviderEventId("evt-1");
        event.setRawBody("{\"immutable\":true}");
        event.setSignatureDigest("digest");
        event.setStatus(status);
        return event;
    }

    private String selectSql(Class<?> mapper, String method) {
        for (var candidate : mapper.getMethods()) if (candidate.getName().equals(method)) {
            return candidate.getAnnotation(org.apache.ibatis.annotations.Select.class).value()[0];
        }
        throw new AssertionError(method);
    }
}
