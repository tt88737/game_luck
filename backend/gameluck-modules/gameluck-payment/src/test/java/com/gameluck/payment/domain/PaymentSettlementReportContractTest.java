package com.gameluck.payment.domain;

import com.gameluck.payment.domain.bo.PaymentSettlementReportQueryBo;
import com.gameluck.payment.domain.vo.PaymentSettlementReportCurrencyTotalVo;
import com.gameluck.payment.domain.vo.PaymentSettlementReportPageVo;
import com.gameluck.payment.domain.vo.PaymentSettlementReportRowVo;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("local")
class PaymentSettlementReportContractTest {

    @Test
    void queryUsesRequiredUtcDateBoundsAndOptionalDimensions() throws Exception {
        assertFields(PaymentSettlementReportQueryBo.class, Map.of(
            "startDate", LocalDate.class,
            "endDate", LocalDate.class,
            "providerCode", String.class,
            "currencyCode", String.class));
        assertThat(field(PaymentSettlementReportQueryBo.class, "startDate").getAnnotation(NotNull.class)).isNotNull();
        assertThat(field(PaymentSettlementReportQueryBo.class, "endDate").getAnnotation(NotNull.class)).isNotNull();
    }

    @Test
    void reportRowsKeepMoneyStringSafeAndDatesIsoCompatible() throws Exception {
        assertFields(PaymentSettlementReportRowVo.class, Map.ofEntries(
            Map.entry("reportDate", LocalDate.class),
            Map.entry("providerCode", String.class),
            Map.entry("currencyCode", String.class),
            Map.entry("batchCount", Long.class),
            Map.entry("eventCount", Long.class),
            Map.entry("paymentEventCount", Long.class),
            Map.entry("refundEventCount", Long.class),
            Map.entry("chargebackEventCount", Long.class),
            Map.entry("grossPayment", String.class),
            Map.entry("refundAmount", String.class),
            Map.entry("chargebackAmount", String.class),
            Map.entry("totalFee", String.class),
            Map.entry("netSettlement", String.class),
            Map.entry("negativeNet", Boolean.class),
            Map.entry("earliestPeriodStart", Date.class),
            Map.entry("latestPeriodEnd", Date.class),
            Map.entry("latestCloseTime", Date.class)));
    }

    @Test
    void pageContainsRowsTotalCurrencyTotalsAndGeneratedTime() throws Exception {
        assertFields(PaymentSettlementReportCurrencyTotalVo.class, Map.ofEntries(
            Map.entry("currencyCode", String.class),
            Map.entry("batchCount", Long.class),
            Map.entry("eventCount", Long.class),
            Map.entry("paymentEventCount", Long.class),
            Map.entry("refundEventCount", Long.class),
            Map.entry("chargebackEventCount", Long.class),
            Map.entry("grossPayment", String.class),
            Map.entry("refundAmount", String.class),
            Map.entry("chargebackAmount", String.class),
            Map.entry("totalFee", String.class),
            Map.entry("netSettlement", String.class)));
        assertFields(PaymentSettlementReportPageVo.class, Map.of(
            "rows", List.class,
            "total", long.class,
            "currencyTotals", List.class,
            "generatedAt", Date.class));
        assertListElement(PaymentSettlementReportPageVo.class, "rows", PaymentSettlementReportRowVo.class);
        assertListElement(PaymentSettlementReportPageVo.class, "currencyTotals", PaymentSettlementReportCurrencyTotalVo.class);
    }

    @Test
    void sqlAndMessageBundlesDefineStableReportMetadata() throws Exception {
        String walletSql = read("backend/script/sql/gameluck_wallet.sql");
        assertThat(walletSql)
            .contains("DELETE FROM sys_menu WHERE menu_id IN (20331,20332,20333,20334,2033)")
            .contains("(2033,'\u652f\u4ed8\u7ed3\u7b97',1900,7,'payment-settlement'")
            .contains("DELETE FROM sys_menu WHERE menu_id IN (20341,20342,20343,2034)")
            .contains("(2034,'\u652f\u4ed8\u7ed3\u7b97\u62a5\u8868',1900,8,'payment-settlement-report','payment/payment-settlement-report/index'")
            .contains("payment:settlementReport:list", "payment:settlementReport:query", "payment:settlementReport:export")
            .contains("'chart'");

        for (String file : new String[]{"messages.properties", "messages_en_US.properties", "messages_zh_CN.properties"}) {
            String messages = read("backend/gameluck-admin/src/main/resources/i18n/" + file);
            assertThat(messages).as(file).contains(
                "payment.settlementReport.date.invalid=", "payment.settlementReport.date.future=",
                "payment.settlementReport.provider.invalid=", "payment.settlementReport.currency.invalid=",
                "payment.settlementReport.export.tooLarge=", "payment.settlementReport.group.notFound=");
        }
    }

    private static void assertFields(Class<?> type, Map<String, Class<?>> expected) {
        assertThat(type.getDeclaredFields()).extracting(Field::getName).containsExactlyInAnyOrderElementsOf(expected.keySet());
        expected.forEach((name, fieldType) -> assertThat(field(type, name).getType()).as(type.getSimpleName() + "." + name)
            .isEqualTo(fieldType));
    }

    private static void assertListElement(Class<?> type, String fieldName, Class<?> elementType) {
        ParameterizedType genericType = (ParameterizedType) field(type, fieldName).getGenericType();
        assertThat(genericType.getActualTypeArguments()).containsExactly(elementType);
    }

    private static Field field(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name);
        } catch (NoSuchFieldException exception) {
            throw new AssertionError("Missing field " + type.getSimpleName() + "." + name, exception);
        }
    }

    private static String read(String relativePath) throws Exception {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve(relativePath))) current = current.getParent();
        if (current == null) throw new IllegalStateException("Repository root not found for " + relativePath);
        return Files.readString(current.resolve(relativePath));
    }
}
