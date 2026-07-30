package com.gameluck.payment.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.net.URISyntaxException;

@Component
@Validated
@ConfigurationProperties(prefix = "payment.providers")
public class PaymentProviderProperties {

    @Valid
    private final Simulated simulated = new Simulated();

    public Simulated getSimulated() {
        return simulated;
    }

    public static class Simulated {

        private boolean enabled;
        private String secret;
        private String checkoutBaseUrl;
        private String webhookBaseUrl;
        @Positive
        private long sessionTtlMinutes = 15;

        @PositiveOrZero
        private long signatureToleranceSeconds = 300;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getCheckoutBaseUrl() {
            return checkoutBaseUrl;
        }

        public void setCheckoutBaseUrl(String checkoutBaseUrl) {
            this.checkoutBaseUrl = checkoutBaseUrl;
        }

        public String getWebhookBaseUrl() {
            return webhookBaseUrl;
        }

        public void setWebhookBaseUrl(String webhookBaseUrl) {
            this.webhookBaseUrl = webhookBaseUrl;
        }

        public long getSessionTtlMinutes() {
            return sessionTtlMinutes;
        }

        public void setSessionTtlMinutes(long sessionTtlMinutes) {
            this.sessionTtlMinutes = sessionTtlMinutes;
        }

        public long getSignatureToleranceSeconds() {
            return signatureToleranceSeconds;
        }

        public void setSignatureToleranceSeconds(long signatureToleranceSeconds) {
            this.signatureToleranceSeconds = signatureToleranceSeconds;
        }

        @AssertTrue(message = "enabled simulated provider requires a secret and absolute HTTP(S) endpoints")
        public boolean isValidWhenEnabled() {
            if (!enabled) {
                return true;
            }
            return secret != null && !secret.isBlank()
                && isAbsoluteHttpUrl(checkoutBaseUrl)
                && isAbsoluteHttpUrl(webhookBaseUrl);
        }

        private static boolean isAbsoluteHttpUrl(String value) {
            if (value == null || value.isBlank()) {
                return false;
            }
            try {
                URI uri = new URI(value);
                return uri.isAbsolute()
                    && uri.getHost() != null
                    && uri.getUserInfo() == null
                    && uri.getQuery() == null
                    && uri.getFragment() == null
                    && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
            } catch (URISyntaxException exception) {
                return false;
            }
        }
    }
}
