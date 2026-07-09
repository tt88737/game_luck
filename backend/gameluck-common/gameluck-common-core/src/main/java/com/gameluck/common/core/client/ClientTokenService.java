package com.gameluck.common.core.client;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class ClientTokenService {

    private static final String PREFIX = "client:";
    private static final long EXPIRES_IN = 7200L;

    public String issue(Long memberId) {
        String raw = PREFIX + memberId + ":" + (Instant.now().getEpochSecond() + EXPIRES_IN);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public Long requireMemberId(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new ServiceException(MessageUtils.message("client.auth.required"));
        }
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = raw.split(":");
            if (parts.length != 3 || !PREFIX.substring(0, PREFIX.length() - 1).equals(parts[0])) {
                throw new IllegalArgumentException("invalid token");
            }
            long expiresAt = Long.parseLong(parts[2]);
            if (expiresAt < Instant.now().getEpochSecond()) {
                throw new ServiceException(MessageUtils.message("client.auth.expired"));
            }
            return Long.parseLong(parts[1]);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException(MessageUtils.message("client.auth.required"));
        }
    }

    public Long expiresIn() {
        return EXPIRES_IN;
    }
}
