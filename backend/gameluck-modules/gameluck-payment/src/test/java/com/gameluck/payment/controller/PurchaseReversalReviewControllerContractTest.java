package com.gameluck.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.log.annotation.Log;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PurchaseReversalReviewControllerContractTest {

    @Test
    @Tag("local")
    void exposesExactRoutesPermissionsAndMutationLogs() throws Exception {
        assertEquals("/payment/purchase-reversal-review",
            PurchaseReversalReviewController.class.getAnnotation(RequestMapping.class).value()[0]);
        assertGet("list", "payment:reversalReview:list", "/list");
        assertGet("getInfo", "payment:reversalReview:query", "/{reversalNo}");
        assertPost("retry", "payment:reversalReview:retry", "/{reversalNo}/retry");
        assertPost("acceptLoss", "payment:reversalReview:acceptLoss", "/{reversalNo}/accept-loss");
    }

    private void assertGet(String name, String permission, String path) {
        Method method = method(name);
        assertEquals(permission, method.getAnnotation(SaCheckPermission.class).value()[0]);
        assertEquals(path, method.getAnnotation(GetMapping.class).value()[0]);
    }

    private void assertPost(String name, String permission, String path) {
        Method method = method(name);
        assertEquals(permission, method.getAnnotation(SaCheckPermission.class).value()[0]);
        assertEquals(path, method.getAnnotation(PostMapping.class).value()[0]);
        assertNotNull(method.getAnnotation(Log.class));
    }

    private Method method(String name) {
        for (Method method : PurchaseReversalReviewController.class.getDeclaredMethods()) {
            if (method.getName().equals(name)) return method;
        }
        throw new AssertionError("Missing method " + name);
    }
}
