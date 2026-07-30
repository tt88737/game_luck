package com.gameluck.payment.service.reconciliation;

import com.gameluck.payment.enums.PaymentReconciliationIssueType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ReconciliationDifference(
    PaymentReconciliationIssueType issueType,
    String field,
    Object expected,
    Object actual
) {
    public ReconciliationDifference {
        Objects.requireNonNull(issueType, "issueType");
        Objects.requireNonNull(field, "field");
        expected = immutableValue(expected);
        actual = immutableValue(actual);
    }

    private static Object immutableValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            map.forEach((key, nested) -> copy.put(key, immutableValue(nested)));
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            list.forEach(nested -> copy.add(immutableValue(nested)));
            return Collections.unmodifiableList(copy);
        }
        return value;
    }
}
