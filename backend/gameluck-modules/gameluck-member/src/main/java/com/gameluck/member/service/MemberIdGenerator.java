package com.gameluck.member.service;

import cn.hutool.core.util.IdUtil;
import org.springframework.stereotype.Component;

/**
 * Generates public member IDs stored in member_no.
 */
@Component
public class MemberIdGenerator {

    private static final String PREFIX = "GL";
    private static final int MIN_DIGITS = 6;
    private static final long MODULUS = 1_000_000_000L;

    public String next() {
        long numeric = Math.abs(IdUtil.getSnowflakeNextId() % MODULUS);
        if (numeric == 0) {
            numeric = 1;
        }
        return format(numeric);
    }

    public String format(long numeric) {
        if (numeric < 0) {
            throw new IllegalArgumentException("Member ID numeric value must not be negative");
        }
        String value = Long.toString(numeric);
        if (value.length() < MIN_DIGITS) {
            value = "0".repeat(MIN_DIGITS - value.length()) + value;
        }
        return PREFIX + value;
    }
}
