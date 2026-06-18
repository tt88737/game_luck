package com.tangluck.promotion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
class RewardPolicyParser {
    private final ObjectMapper objectMapper;

    RewardPolicyParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    List<RewardDto> parse(String json) {
        try {
            return objectMapper.readValue(normalize(json), new TypeReference<List<RewardDto>>() {
            });
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid reward policy json", exception);
        }
    }

    List<String> parseRegions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(normalize(json), new TypeReference<List<String>>() {
            });
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid region json", exception);
        }
    }

    private String normalize(String json) throws Exception {
        var trimmed = json == null ? "" : json.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return objectMapper.readValue(trimmed, String.class);
        }
        return trimmed;
    }
}
