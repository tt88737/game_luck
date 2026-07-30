package com.gameluck.common.core.client;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

@Service
public class ClientTokenService {

    private static final String PREFIX = "client:";
    private static final long EXPIRES_IN = 7200L;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TEST_SECRET = "TEST-ONLY-client-token-secret";
    private static final int MAX_TOKEN_LENGTH = 1024;
    private final byte[] secret;
    private final Clock clock;

    /** Test-only constructor retained for existing unit tests; Spring uses the annotated constructor. */
    public ClientTokenService() {
        this(TEST_SECRET, Clock.systemUTC());
    }

    @Autowired
    public ClientTokenService(@Value("${client.auth.token-secret:}") String secret) {
        this(secret, Clock.systemUTC());
    }

    public ClientTokenService(String secret, Clock clock) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("client.auth.token-secret must be configured");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    public String issue(Long memberId) {
        String raw = PREFIX + memberId + ":" + (clock.instant().getEpochSecond() + EXPIRES_IN);
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(payload.getBytes(StandardCharsets.US_ASCII)));
        return payload + "." + signature;
    }

    public Long requireMemberId(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new ServiceException(MessageUtils.message("client.auth.required"));
        }
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        try {
            if (token.length() > MAX_TOKEN_LENGTH) throw new IllegalArgumentException("invalid token");
            String[] signed = token.split("\\.", -1);
            if (signed.length != 2 || signed[0].isBlank() || signed[1].isBlank()) throw new IllegalArgumentException("invalid token");
            byte[] supplied = Base64.getUrlDecoder().decode(signed[1]);
            byte[] expected = hmac(signed[0].getBytes(StandardCharsets.US_ASCII));
            if (!MessageDigest.isEqual(expected, supplied)) throw new IllegalArgumentException("invalid token");
            String raw = new String(Base64.getUrlDecoder().decode(signed[0]), StandardCharsets.UTF_8);
            String[] parts = raw.split(":", -1);
            if (parts.length != 3 || !"client".equals(parts[0])) {
                throw new IllegalArgumentException("invalid token");
            }
            long expiresAt = Long.parseLong(parts[2]);
            if (expiresAt <= clock.instant().getEpochSecond()) {
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

    private byte[] hmac(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to sign client token", exception);
        }
    }
}
