package com.gameluck.payment.domain;

import com.gameluck.payment.domain.bo.PaymentSessionAdminBo;
import com.gameluck.payment.domain.bo.PaymentWebhookEventAdminBo;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PaymentProviderAdminBoValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test @Tag("local")
    void acceptsExactDdlBoundaries() {
        PaymentSessionAdminBo bo = new PaymentSessionAdminBo();
        bo.setSessionNo("s".repeat(64)); bo.setPurchaseOrderNo("o".repeat(64));
        bo.setProviderSessionNo("p".repeat(128)); bo.setMemberNo("m".repeat(64));
        bo.setProviderCode("SIMULATED"); bo.setStatus("SUCCEEDED"); bo.setPayCurrencyCode("USDT"); bo.setMemberId(1L);
        assertTrue(validator.validate(bo).isEmpty());
    }

    @Test @Tag("local")
    void rejectsInvalidEnumsMemberAndRangesAndOverlongIdentifiers() {
        PaymentSessionAdminBo session = new PaymentSessionAdminBo();
        session.setSessionNo("x".repeat(65)); session.setMemberId(0L); session.setProviderCode("bad provider");
        session.setStatus("BOGUS"); session.setPayCurrencyCode("usd!");
        session.setBeginTime(new Date(2)); session.setEndTime(new Date(1));
        assertTrue(validator.validate(session).size() >= 6);
        PaymentWebhookEventAdminBo event = new PaymentWebhookEventAdminBo();
        event.setProviderEventId("x".repeat(129)); event.setProviderCode("bad provider"); event.setStatus("BOGUS");
        event.setEventType("BOGUS"); event.setBeginTime(new Date(2)); event.setEndTime(new Date(1));
        assertTrue(validator.validate(event).size() >= 5);
    }

    @Test @Tag("local")
    void everyAdminFilterViolationUsesTask6I18nMessages() {
        PaymentSessionAdminBo session = new PaymentSessionAdminBo();
        session.setSessionNo("x".repeat(65)); session.setMemberId(0L);
        session.setProviderCode("bad provider"); session.setStatus("BOGUS");
        session.setPayCurrencyCode("usd!");
        Set<String> templates = validator.validate(session).stream()
            .map(v -> v.getMessageTemplate()).collect(Collectors.toSet());
        assertEquals(Set.of("{payment.provider.admin.filter.length.invalid}",
            "{payment.provider.admin.member.id.invalid}",
            "{payment.provider.admin.filter.format.invalid}",
            "{payment.provider.admin.filter.enum.invalid}"), templates);
    }
}
