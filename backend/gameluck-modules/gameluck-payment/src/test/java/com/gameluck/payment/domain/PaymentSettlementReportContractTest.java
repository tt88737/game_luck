package com.gameluck.payment.domain;

import com.gameluck.payment.domain.bo.PaymentSettlementReportQueryBo;
import com.gameluck.payment.domain.vo.PaymentSettlementReportCurrencyTotalVo;
import com.gameluck.payment.domain.vo.PaymentSettlementReportPageVo;
import com.gameluck.payment.domain.vo.PaymentSettlementReportRowVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("local")
class PaymentSettlementReportContractTest {

    @Test
    void queryAndResponseContractsPreserveDatesIdentifiersAndMoney() throws Exception {
        assertFields(PaymentSettlementReportQueryBo.class, Map.of(
            "startDate", LocalDate.class,
            "endDate", LocalDate.class,
            "providerCode", String.class,
            "currencyCode", String.class));

        assertFields(PaymentSettlementReportRowVo.class, Map.ofEntries(
            Map.entry("settlementDate", String.class),
            Map.entry("providerCode", String.class),
            Map.entry("currencyCode", String.class),
            Map.entry("batchCount", Integer.class),
            Map.entry("eventCount", Integer.class),
            Map.entry("paymentCount", Integer.class),
            Map.entry("refundCount", Integer.class),
            Map.entry("chargebackCount", Integer.class),
            Map.entry("grossPayment", String.class),
            Map.entry("refundAmount", String.class),
            Map.entry("chargebackAmount", String.class),
            Map.entry("totalFee", String.class),
            Map.entry("netSettlement", String.class),
            Map.entry("negativeNet", Boolean.class),
            Map.entry("earliestPeriodStart", String.class),
            Map.entry("latestPeriodEnd", String.class),
            Map.entry("latestCloseTime", String.class)));

        assertFields(PaymentSettlementReportCurrencyTotalVo.class, Map.ofEntries(
            Map.entry("currencyCode", String.class),
            Map.entry("batchCount", Integer.class),
            Map.entry("eventCount", Integer.class),
            Map.entry("paymentCount", Integer.class),
            Map.entry("refundCount", Integer.class),
            Map.entry("chargebackCount", Integer.class),
            Map.entry("grossPayment", String.class),
            Map.entry("refundAmount", String.class),
            Map.entry("chargebackAmount", String.class),
            Map.entry("totalFee", String.class),
            Map.entry("netSettlement", String.class)));

        assertFields(PaymentSettlementReportPageVo.class, Map.of(
            "rows", List.class,
            "total", Long.class,
            "currencyTotals", List.class,
            "generatedAt", String.class));
    }

    @Test
    void sqlMetadataDefinesTheReportPageAfterSettlementWithDedicatedPermissions() throws Exception {
        String walletSql = read("backend/script/sql/gameluck_wallet.sql");

        assertThat(walletSql)
            .contains("DELETE FROM sys_menu WHERE menu_id IN (20341,20342,20343,2034)")
            .contains("(2034,'支付结算报表',1900,8,'payment-settlement-report','payment/payment-settlement-report/index'")
            .contains("payment:settlementReport:list", "payment:settlementReport:query",
                "payment:settlementReport:export");
    }

    @Test
    void allMessageBundlesContainStableReportFailures() throws Exception {
        for (String file : new String[]{"messages.properties", "messages_en_US.properties", "messages_zh_CN.properties"}) {
            String messages = read("backend/gameluck-admin/src/main/resources/i18n/" + file);
            assertThat(messages).as(file).contains(
                "payment.settlementReport.date.invalid=",
                "payment.settlementReport.date.future=",
                "payment.settlementReport.provider.invalid=",
                "payment.settlementReport.currency.invalid=",
                "payment.settlementReport.export.tooLarge=",
                "payment.settlementReport.group.notFound=");
        }
    }

    private static void assertFields(Class<?> type, Map<String, Class<?>> expected) throws Exception {
        assertThat(type.getDeclaredFields()).extracting(Field::getName)
            .containsExactlyInAnyOrderElementsOf(expected.keySet());
        for (Map.Entry<String, Class<?>> entry : expected.entrySet()) {
            assertThat(type.getDeclaredField(entry.getKey()).getType()).as(entry.getKey())
                .isEqualTo(entry.getValue());
        }
    }

    private static String read(String relativePath) throws Exception {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve(relativePath))) current = current.getParent();
        if (current == null) throw new IllegalStateException("Repository root not found for " + relativePath);
        return Files.readString(current.resolve(relativePath));
    }
}
