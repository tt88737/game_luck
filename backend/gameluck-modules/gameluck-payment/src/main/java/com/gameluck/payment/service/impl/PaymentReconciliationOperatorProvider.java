package com.gameluck.payment.service.impl;

import com.gameluck.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Component;

@Component
public class PaymentReconciliationOperatorProvider {
    public Operator current() { return new Operator(LoginHelper.getUserId(), LoginHelper.getUsername()); }
    public record Operator(Long id, String name) { }
}
