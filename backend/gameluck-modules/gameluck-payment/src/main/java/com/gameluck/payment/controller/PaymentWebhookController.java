package com.gameluck.payment.controller;

import com.gameluck.payment.domain.vo.PaymentWebhookAckVo;
import com.gameluck.payment.service.IPaymentWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@RestController
public class PaymentWebhookController {

    private final IPaymentWebhookService paymentWebhookService;

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Void> handleMissingRequestHeader() {
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/payment/webhooks/{providerCode}")
    public ResponseEntity<PaymentWebhookAckVo> receive(
        @PathVariable String providerCode,
        @RequestHeader("X-Payment-Timestamp") String timestamp,
        @RequestHeader("X-Payment-Signature") String signature,
        @RequestBody byte[] rawBody) {
        try {
            return ResponseEntity.ok(paymentWebhookService.receive(providerCode, timestamp, signature, rawBody));
        } catch (ResponseStatusException exception) {
            if (HttpStatus.UNAUTHORIZED.equals(exception.getStatusCode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            throw exception;
        }
    }
}
