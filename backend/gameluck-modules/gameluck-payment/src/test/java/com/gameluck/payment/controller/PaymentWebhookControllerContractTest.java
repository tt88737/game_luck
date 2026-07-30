package com.gameluck.payment.controller;

import com.gameluck.payment.domain.vo.PaymentWebhookAckVo;
import com.gameluck.payment.service.IPaymentWebhookService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("local")
class PaymentWebhookControllerContractTest {

    @Test
    void receivesExactRawBytesAndRequiredHeaders() throws Exception {
        Method method = PaymentWebhookController.class.getDeclaredMethod(
            "receive", String.class, String.class, String.class, byte[].class);
        assertEquals("/payment/webhooks/{providerCode}", method.getAnnotation(PostMapping.class).value()[0]);
        Parameter[] parameters = method.getParameters();
        assertNotNull(parameters[0].getAnnotation(PathVariable.class));
        assertEquals("X-Payment-Timestamp", parameters[1].getAnnotation(RequestHeader.class).value());
        assertEquals("X-Payment-Signature", parameters[2].getAnnotation(RequestHeader.class).value());
        assertNotNull(parameters[3].getAnnotation(RequestBody.class));

        IPaymentWebhookService service = mock(IPaymentWebhookService.class);
        PaymentWebhookController controller = new PaymentWebhookController(service);
        byte[] raw = new byte[]{0x7b, (byte) 0xc3, 0x28, 0x7d};
        PaymentWebhookAckVo ack = new PaymentWebhookAckVo("evt-1", "PROCESSED");
        when(service.receive("SIMULATED", "100", "abc", raw)).thenReturn(ack);

        ResponseEntity<PaymentWebhookAckVo> response = controller.receive("SIMULATED", "100", "abc", raw);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ack, response.getBody());
        verify(service).receive("SIMULATED", "100", "abc", raw);
        assertArrayEquals(new byte[]{0x7b, (byte) 0xc3, 0x28, 0x7d}, raw);
    }

    @Test
    void invalidSignaturePropagatesHttp401() {
        IPaymentWebhookService service = mock(IPaymentWebhookService.class);
        PaymentWebhookController controller = new PaymentWebhookController(service);
        when(service.receive("SIMULATED", "100", "bad", new byte[]{1}))
            .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid payment webhook"));

        ResponseEntity<PaymentWebhookAckVo> response =
            controller.receive("SIMULATED", "100", "bad", new byte[]{1});

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(null, response.getBody());
    }
}
