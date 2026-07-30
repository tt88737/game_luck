package com.gameluck.payment.domain;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentProviderAdminI18nContractTest {
    @Test @Tag("local")
    void task6KeysExistExactlyOnceInAllThreeBundles() throws Exception {
        List<String> keys = List.of("payment.session.not.exists", "payment.webhook.event.not.exists",
            "payment.webhook.event.retry.status.invalid", "payment.provider.admin.time.range.invalid",
            "payment.provider.admin.filter.length.invalid", "payment.provider.admin.filter.format.invalid",
            "payment.provider.admin.filter.enum.invalid", "payment.provider.admin.member.id.invalid",
            "payment.webhook.event.retry.persistence.failed");
        for (String file : List.of("messages.properties", "messages_zh_CN.properties", "messages_en_US.properties")) {
            String text = Files.readString(locate(file));
            for (String key : keys) assertEquals(1, text.lines().filter(line -> line.startsWith(key + "=")).count(), file + ":" + key);
        }
    }

    private Path locate(String file) {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("gameluck-admin/src/main/resources/i18n/" + file);
            if (Files.exists(candidate)) return candidate;
            current = current.getParent();
        }
        throw new AssertionError(file + " not found");
    }
}
