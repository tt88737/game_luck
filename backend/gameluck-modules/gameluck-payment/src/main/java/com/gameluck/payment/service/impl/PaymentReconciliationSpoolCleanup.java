package com.gameluck.payment.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Component
public class PaymentReconciliationSpoolCleanup {
    private final PaymentReconciliationDeleteOnExitRegistrar registrar;
    @Autowired
    public PaymentReconciliationSpoolCleanup(PaymentReconciliationDeleteOnExitRegistrar registrar) { this.registrar = registrar; }
    PaymentReconciliationSpoolCleanup() { this(new PaymentReconciliationDeleteOnExitRegistrar()); }
    public void cleanup(Path file, Path directory, String fileId) {
        try {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        } catch (IOException e) {
            registrar.register(directory);
            registrar.register(file);
            log.warn("Reconciliation spool cleanup deferred for file id {}", fileId);
        }
    }
}
