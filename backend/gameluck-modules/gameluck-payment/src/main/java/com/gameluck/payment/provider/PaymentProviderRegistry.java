package com.gameluck.payment.provider;

import com.gameluck.payment.config.PaymentProviderProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class PaymentProviderRegistry {

    private final Map<String, PaymentProviderAdapter> adapters;
    private final PaymentProviderProperties properties;

    public PaymentProviderRegistry(List<PaymentProviderAdapter> adapters, PaymentProviderProperties properties) {
        this.properties = properties;
        Map<String, PaymentProviderAdapter> indexed = new HashMap<>();
        for (PaymentProviderAdapter adapter : adapters) {
            String code = normalize(adapter.providerCode());
            if (indexed.putIfAbsent(code, adapter) != null) {
                throw new IllegalStateException("Duplicate payment provider: " + code);
            }
        }
        this.adapters = Map.copyOf(indexed);
    }

    public PaymentProviderAdapter resolve(String providerCode) {
        String code = normalize(providerCode);
        PaymentProviderAdapter adapter = adapters.get(code);
        if (adapter == null) {
            throw new IllegalArgumentException("Unknown payment provider: " + code);
        }
        if ("SIMULATED".equals(code) && !properties.getSimulated().isEnabled()) {
            throw new IllegalStateException("Payment provider is disabled: " + code);
        }
        return adapter;
    }

    private static String normalize(String providerCode) {
        if (providerCode == null || providerCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment provider code is required");
        }
        return providerCode.trim().toUpperCase(Locale.ROOT);
    }
}
