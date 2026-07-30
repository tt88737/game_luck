package com.gameluck.payment.domain;

import com.gameluck.payment.enums.PurchaseOrderStatus;
import com.gameluck.payment.enums.PurchaseReversalStatus;
import com.gameluck.payment.enums.PurchaseReversalType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseReversalContractTest {

    @Test
    @Tag("local")
    void exposesReviewAndReversalStatuses() {
        assertEquals("REFUND_REVIEW", PurchaseOrderStatus.REFUND_REVIEW.name());
        assertEquals("CHARGEBACK_REVIEW", PurchaseOrderStatus.CHARGEBACK_REVIEW.name());
        assertEquals("REFUND", PurchaseReversalType.REFUND.name());
        assertEquals("CHARGEBACK", PurchaseReversalType.CHARGEBACK.name());
        assertEquals("PROCESSING", PurchaseReversalStatus.PROCESSING.name());
        assertEquals("COMPLETED", PurchaseReversalStatus.COMPLETED.name());
        assertEquals("REVIEW_REQUIRED", PurchaseReversalStatus.REVIEW_REQUIRED.name());
    }

    @Test
    @Tag("local")
    void usesBigDecimalForEveryReversalItemAmount() throws Exception {
        assertEquals(BigDecimal.class, PurchaseReversalItem.class.getDeclaredField("requiredAmount").getType());
        assertEquals(BigDecimal.class, PurchaseReversalItem.class.getDeclaredField("availableAmount").getType());
        assertEquals(BigDecimal.class, PurchaseReversalItem.class.getDeclaredField("recoveredAmount").getType());
        assertEquals(BigDecimal.class, PurchaseReversalItem.class.getDeclaredField("shortfallAmount").getType());
    }

    @Test
    @Tag("local")
    void schemaDefinesReversalTablesAndTenantScopedUniqueKeys() throws Exception {
        String sql = Files.readString(findWalletSchema())
            .replaceAll("\\s+", " ")
            .toLowerCase();

        assertTrue(sql.contains("create table if not exists gl_purchase_reversal ("));
        assertTrue(sql.contains("create table if not exists gl_purchase_reversal_item ("));
        assertTrue(sql.contains("unique key uk_gl_purchase_reversal_01 (tenant_id, reversal_no)"));
        assertTrue(sql.contains("unique key uk_gl_purchase_reversal_02 (tenant_id, event_key)"));
        assertTrue(sql.contains("unique key uk_gl_purchase_reversal_item_01 (tenant_id, reversal_no, currency_code)"));
        assertTrue(sql.contains("required_amount decimal(20,8)"));
        assertTrue(sql.contains("available_amount decimal(20,8)"));
        assertTrue(sql.contains("recovered_amount decimal(20,8)"));
        assertTrue(sql.contains("shortfall_amount decimal(20,8)"));
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
