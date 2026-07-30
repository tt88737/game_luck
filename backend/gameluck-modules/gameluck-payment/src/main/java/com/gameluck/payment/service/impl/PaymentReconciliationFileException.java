package com.gameluck.payment.service.impl;

final class PaymentReconciliationFileException extends RuntimeException {
    private final String code;
    PaymentReconciliationFileException(String code) { super(code); this.code = code; }
    String code() { return code; }
}
