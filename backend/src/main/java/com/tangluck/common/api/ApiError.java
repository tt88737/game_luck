package com.tangluck.common.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record ApiError(
        String code,
        String message,
        @JsonProperty("trace_id") String traceId,
        Map<String, Object> details
) {
}
