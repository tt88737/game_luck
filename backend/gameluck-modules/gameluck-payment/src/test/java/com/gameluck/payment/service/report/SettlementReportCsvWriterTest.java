package com.gameluck.payment.service.report;

import com.gameluck.payment.domain.vo.PaymentSettlementReportRowVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("local")
class SettlementReportCsvWriterTest {
    @Test
    void writesBomFixedHeaderExactMoneyAndStructuredEscaping() {
        PaymentSettlementReportRowVo row = row("SIM,\"ULATED\n", "10.000000");
        byte[] bytes = new SettlementReportCsvWriter().write(List.of(row));
        assertThat(bytes).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        String csv = new String(bytes, 3, bytes.length - 3, java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv.lines().findFirst().orElseThrow().split(",")).hasSize(17);
        assertThat(csv).contains("report_date,provider_code,currency_code,batch_count,event_count")
            .contains("\"SIM,\"\"ULATED")
            .contains("10.000000")
            .contains("2026-07-29T01:02:03Z");
    }

    @Test
    void prefixesSpreadsheetFormulaTextAfterLeadingWhitespace() {
        assertThat(SettlementReportCsvWriter.safeText("  =SUM(A1:A2)")).isEqualTo("'  =SUM(A1:A2)");
        assertThat(SettlementReportCsvWriter.safeText("+cmd")).isEqualTo("'+cmd");
        assertThat(SettlementReportCsvWriter.safeText("-cmd")).isEqualTo("'-cmd");
        assertThat(SettlementReportCsvWriter.safeText("@cmd")).isEqualTo("'@cmd");
        assertThat(SettlementReportCsvWriter.safeText("SIMULATED")).isEqualTo("SIMULATED");
        assertThat(SettlementReportCsvWriter.safeText(null)).isEmpty();
    }

    private static PaymentSettlementReportRowVo row(String provider, String net) {
        PaymentSettlementReportRowVo value = new PaymentSettlementReportRowVo();
        value.setReportDate(LocalDate.parse("2026-07-29")); value.setProviderCode(provider); value.setCurrencyCode("USD");
        value.setBatchCount(1L); value.setEventCount(3L); value.setPaymentEventCount(1L); value.setRefundEventCount(1L);
        value.setChargebackEventCount(1L); value.setGrossPayment("30.000000"); value.setRefundAmount("10.000000");
        value.setChargebackAmount("10.000000"); value.setTotalFee("16.770000"); value.setNetSettlement(net);
        value.setNegativeNet(net.startsWith("-")); value.setEarliestPeriodStart(Date.from(Instant.parse("2026-07-29T01:02:03Z")));
        value.setLatestPeriodEnd(Date.from(Instant.parse("2026-07-29T02:02:03Z")));
        value.setLatestCloseTime(Date.from(Instant.parse("2026-07-29T03:02:03Z"))); return value;
    }
}
