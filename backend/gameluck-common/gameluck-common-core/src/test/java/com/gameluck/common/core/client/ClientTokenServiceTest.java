package com.gameluck.common.core.client;

import com.gameluck.common.core.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@Tag("local")
class ClientTokenServiceTest {
    private static final String SECRET = "unit-test-client-token-secret";
    private static final Instant NOW = Instant.parse("2026-07-27T08:00:00Z");

    @Test
    void validSignedTokenReturnsMember() {
        ClientTokenService service = service(NOW);
        assertEquals(42L, service.requireMemberId("Bearer " + service.issue(42L)));
    }

    @Test
    void rejectsForgedUnsignedAndTamperedTokens() {
        ClientTokenService service = service(NOW);
        String forged = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(("client:999:" + NOW.plusSeconds(600).getEpochSecond()).getBytes(StandardCharsets.UTF_8));
        assertThrows(ServiceException.class, () -> service.requireMemberId(forged));

        String token = service.issue(42L);
        String[] parts = token.split("\\.");
        String changedPayload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(("client:43:" + NOW.plusSeconds(7200).getEpochSecond()).getBytes(StandardCharsets.UTF_8));
        assertThrows(ServiceException.class, () -> service.requireMemberId(changedPayload + "." + parts[1]));
        String changedSignature = parts[1].substring(0, parts[1].length() - 1) + (parts[1].endsWith("A") ? "B" : "A");
        assertThrows(ServiceException.class, () -> service.requireMemberId(parts[0] + "." + changedSignature));
    }

    @Test
    void rejectsExpiredSignedToken() {
        String token = service(NOW).issue(42L);
        assertThrows(ServiceException.class, () -> service(NOW.plusSeconds(7200)).requireMemberId(token));
        assertThrows(ServiceException.class, () -> service(NOW.plusSeconds(7201)).requireMemberId(token));
    }

    @Test
    void productionConstructorRejectsBlankSecret() {
        assertThrows(IllegalStateException.class, () -> new ClientTokenService(" "));
    }

    private ClientTokenService service(Instant now) {
        return new ClientTokenService(SECRET, Clock.fixed(now, ZoneOffset.UTC));
    }
}
