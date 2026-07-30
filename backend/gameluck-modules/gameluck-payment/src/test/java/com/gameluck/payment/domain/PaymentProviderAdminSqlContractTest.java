package com.gameluck.payment.domain;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class PaymentProviderAdminSqlContractTest {
    private static final Path SQL = locateSql();

    @Test @Tag("local")
    void task6MenuIdsAreGloballyUniqueAndTreeAndPermissionsAreExact() throws Exception {
        String sql = Files.readString(SQL);
        long[] ids = {2030, 20301, 2031, 20311, 20312};
        for (long id : ids) assertEquals(1, occurrences(sql, "(?m)^\\(" + id + ","),
            "menu id must have exactly one final row definition: " + id);
        assertTrue(sql.contains("(2030, '支付会话', 1900, 4"));
        assertTrue(sql.contains("(20301, '支付会话查询', 2030, 1"));
        assertTrue(sql.contains("(2031, '支付回调事件', 1900, 5"));
        assertTrue(sql.contains("(20311, '支付回调事件查询', 2031, 1"));
        assertTrue(sql.contains("(20312, '支付回调事件重试', 2031, 2"));
        for (String permission : new String[]{"payment:paymentSession:list", "payment:paymentSession:query",
            "payment:webhookEvent:list", "payment:webhookEvent:query", "payment:webhookEvent:retry"}) {
            assertEquals(1, occurrences(sql, Pattern.quote(permission)));
        }
        assertTrue(sql.contains("DELETE FROM sys_menu WHERE menu_id IN (20301, 20311, 20312, 2030, 2031)"));
    }

    @Test @Tag("local")
    void task6DictionaryIdsAndValuesAreUniqueAndReplayStable() throws Exception {
        String sql = Files.readString(SQL);
        for (int id = 20037; id <= 20039; id++) assertEquals(1, occurrences(sql, "\\(" + id + ","));
        for (int id = 21276; id <= 21290; id++) assertEquals(1, occurrences(sql, "\\(" + id + ","));
        assertTrue(sql.contains("DELETE FROM sys_dict_data WHERE tenant_id = '000000'"));
        assertTrue(sql.contains("DELETE FROM sys_dict_type WHERE tenant_id = '000000'"));
        for (String value : new String[]{"CREATED", "PENDING", "SUCCEEDED", "FAILED", "CANCELLED", "EXPIRED",
            "RECEIVED", "PROCESSED", "IGNORED", "PAYMENT_SUCCEEDED", "PAYMENT_FAILED", "PAYMENT_CANCELLED",
            "REFUND_SUCCEEDED", "CHARGEBACK_CREATED"}) assertTrue(sql.contains("'" + value + "'"));
    }

    private long occurrences(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        long count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private static Path locateSql() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("script/sql/gameluck_wallet.sql");
            if (Files.exists(candidate)) return candidate;
            current = current.getParent();
        }
        throw new AssertionError("script/sql/gameluck_wallet.sql not found");
    }
}
