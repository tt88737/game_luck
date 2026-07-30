package com.gameluck.payment.service.impl;

import org.springframework.stereotype.Component;
import java.nio.file.Path;

@Component
public class PaymentReconciliationDeleteOnExitRegistrar {
    public void register(Path path) { if (path != null) path.toFile().deleteOnExit(); }
}
