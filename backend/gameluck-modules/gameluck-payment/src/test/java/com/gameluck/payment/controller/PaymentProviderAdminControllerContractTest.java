package com.gameluck.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class PaymentProviderAdminControllerContractTest {

    @Test
    @Tag("local")
    void exposesExactRoutesPermissionsAndRetryAuditLog() {
        assertController(PaymentSessionController.class, "/payment/payment-session",
            "list", "payment:paymentSession:list", "/list",
            "getInfo", "payment:paymentSession:query", "/{id}");
        assertController(PaymentWebhookEventController.class, "/payment/webhook-event",
            "list", "payment:webhookEvent:list", "/list",
            "getInfo", "payment:webhookEvent:query", "/{id}");
        Method retry = method(PaymentWebhookEventController.class, "retry");
        assertEquals("payment:webhookEvent:retry", retry.getAnnotation(SaCheckPermission.class).value()[0]);
        assertEquals("/{id}/retry", retry.getAnnotation(PostMapping.class).value()[0]);
        Log log = retry.getAnnotation(Log.class);
        assertNotNull(log);
        assertEquals("Payment webhook event", log.title());
        assertEquals(BusinessType.UPDATE, log.businessType());
        assertTrue(retry.getGenericReturnType().getTypeName().contains("PaymentWebhookRetryResultVo"));
    }

    @Test
    @Tag("local")
    void controllersDependOnlyOnAdminServiceForDataAccess() {
        for (Class<?> type : new Class<?>[]{PaymentSessionController.class, PaymentWebhookEventController.class}) {
            for (Field field : type.getDeclaredFields()) {
                assertFalse(field.getType().getName().contains(".mapper."));
            }
        }
    }

    private void assertController(Class<?> type, String route, String listName, String listPermission,
                                  String listRoute, String detailName, String detailPermission, String detailRoute) {
        assertEquals(route, type.getAnnotation(RequestMapping.class).value()[0]);
        Method list = method(type, listName);
        assertEquals(listPermission, list.getAnnotation(SaCheckPermission.class).value()[0]);
        assertEquals(listRoute, list.getAnnotation(GetMapping.class).value()[0]);
        Method detail = method(type, detailName);
        assertEquals(detailPermission, detail.getAnnotation(SaCheckPermission.class).value()[0]);
        assertEquals(detailRoute, detail.getAnnotation(GetMapping.class).value()[0]);
    }

    private Method method(Class<?> type, String name) {
        for (Method method : type.getDeclaredMethods()) if (method.getName().equals(name)) return method;
        throw new AssertionError("Missing method " + name);
    }
}
