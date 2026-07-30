package com.gameluck.payment.controller;

import com.gameluck.payment.domain.bo.SimulatedPaymentActionBo;
import com.gameluck.payment.domain.vo.PaymentWebhookAckVo;
import com.gameluck.payment.domain.vo.SimulatedCheckoutVo;
import com.gameluck.payment.enums.PaymentProviderEventType;
import com.gameluck.payment.service.ISimulatedPaymentService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Tag("local")
class SimulatedPaymentControllerContractTest {

    @Test
    void exposesOnlyPathBoundCheckoutActionsAndReplay() throws Exception {
        Method get = SimulatedPaymentController.class.getDeclaredMethod("checkout", String.class);
        assertEquals("/payment/simulated/checkout/{providerSessionNo}", get.getAnnotation(GetMapping.class).value()[0]);
        assertNotNull(get.getParameters()[0].getAnnotation(PathVariable.class));

        Method action = SimulatedPaymentController.class.getDeclaredMethod(
            "action", String.class, SimulatedPaymentActionBo.class);
        assertEquals("/payment/simulated/checkout/{providerSessionNo}/actions",
            action.getAnnotation(PostMapping.class).value()[0]);
        assertNotNull(action.getParameters()[0].getAnnotation(PathVariable.class));
        assertNotNull(action.getParameters()[1].getAnnotation(RequestBody.class));

        Method replay = SimulatedPaymentController.class.getDeclaredMethod("replay", String.class);
        assertEquals("/payment/simulated/checkout/{providerSessionNo}/replay",
            replay.getAnnotation(PostMapping.class).value()[0]);

        RecordComponent[] fields = SimulatedPaymentActionBo.class.getRecordComponents();
        assertEquals(1, fields.length);
        assertEquals("action", fields[0].getName());
        assertEquals(PaymentProviderEventType.class, fields[0].getType());
        assertTrue(Arrays.stream(fields).noneMatch(field ->
            Set.of("amount", "currency", "orderNo", "memberId", "sessionNo").contains(field.getName())));
    }

    @Test
    void delegatesUsingOnlyProviderSessionPath() {
        ISimulatedPaymentService service = mock(ISimulatedPaymentService.class);
        SimulatedPaymentController controller = new SimulatedPaymentController(service);
        SimulatedPaymentActionBo body = new SimulatedPaymentActionBo(PaymentProviderEventType.PAYMENT_SUCCEEDED);

        controller.checkout("SIM-1");
        controller.action("SIM-1", body);
        controller.replay("SIM-1");

        verify(service).getCheckout("SIM-1");
        verify(service).executeAction("SIM-1", body);
        verify(service).replay("SIM-1");
    }
}
