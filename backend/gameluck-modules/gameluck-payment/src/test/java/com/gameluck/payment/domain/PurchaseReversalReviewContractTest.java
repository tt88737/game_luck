package com.gameluck.payment.domain;

import com.gameluck.payment.enums.PurchaseReversalDispositionStatus;
import com.gameluck.payment.enums.PurchaseReversalReviewOperationType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseReversalReviewContractTest {

    @Test
    @Tag("local")
    void exposesExactDispositionAndOperationTypes() {
        assertEquals(List.of("PENDING_REVIEW", "RECOVERY_COMPLETED", "LOSS_ACCEPTED"),
            Arrays.stream(PurchaseReversalDispositionStatus.values()).map(Enum::name).toList());
        assertEquals(List.of("RETRY_INSUFFICIENT", "RETRY_COMPLETED", "LOSS_ACCEPTED"),
            Arrays.stream(PurchaseReversalReviewOperationType.values()).map(Enum::name).toList());
    }

    @Test
    @Tag("local")
    void reversalContainsEveryReviewAuditField() throws Exception {
        assertEquals(String.class, PurchaseReversal.class.getDeclaredField("dispositionStatus").getType());
        assertEquals(Long.class, PurchaseReversal.class.getDeclaredField("reviewedBy").getType());
        assertEquals(String.class, PurchaseReversal.class.getDeclaredField("reviewedName").getType());
        assertEquals(String.class, PurchaseReversal.class.getDeclaredField("reviewNote").getType());
        assertEquals(Date.class, PurchaseReversal.class.getDeclaredField("resolvedTime").getType());
        assertEquals(Integer.class, PurchaseReversal.class.getDeclaredField("retryCount").getType());
        assertEquals(Date.class, PurchaseReversal.class.getDeclaredField("lastRetryTime").getType());
        assertEquals(Integer.class, PurchaseReversal.class.getDeclaredField("version").getType());
    }

    @Test
    @Tag("local")
    void schemaDefinesReviewFieldsLogAndTenantScopedUniqueKeys() throws Exception {
        String sql = Files.readString(findWalletSchema())
            .replaceAll("\\s+", " ")
            .toLowerCase();

        assertTrue(sql.contains("disposition_status varchar(32)"));
        assertTrue(sql.contains("reviewed_by bigint"));
        assertTrue(sql.contains("reviewed_name varchar(100)"));
        assertTrue(sql.contains("review_note varchar(500)"));
        assertTrue(sql.contains("resolved_time datetime"));
        assertTrue(sql.contains("retry_count int"));
        assertTrue(sql.contains("last_retry_time datetime"));
        assertTrue(sql.contains("version int"));
        assertTrue(sql.contains("create table if not exists gl_purchase_reversal_review_log ("));
        assertTrue(sql.contains("snapshot_json longtext"));
        assertTrue(sql.contains("unique key uk_gl_purchase_reversal_review_log_01 (tenant_id, request_key)"));
        assertTrue(sql.contains("unique key uk_gl_purchase_reversal_review_log_02 (tenant_id, operation_no)"));
    }

    private Path findWalletSchema() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("script", "sql", "gameluck_wallet.sql"));
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate script/sql/gameluck_wallet.sql from user.dir");
    }
}
