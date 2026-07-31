package com.gameluck.payment.service.report;

import com.gameluck.payment.domain.vo.PaymentSettlementReportRowVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("local")
class SettlementReportCsvWriterTest {

    @Test
    void writesBomFixedColumnsEscapedTextAndExactDecimals() {
        PaymentSettlementReportRowVo row = row(" =SUM(A1:A2)\r\n\"SIM,ULATED\"");

        byte[] bytes = new SettlementReportCsvWriter().write(List.of(row));
        String csv = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);

        assertThat(bytes).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(csv.lines().findFirst().orElseThrow().split(",", -1)).hasSize(17);
        assertThat(csv).startsWith("report_date,provider_code,currency_code,batch_count,event_count,")
            .contains("gross_payment,refund_amount,chargeback_amount,total_fee,net_settlement,")
            .contains("\"' =SUM(A1:A2)\r\n\"\"SIM,ULATED\"\"\"")
            .contains("123.450000")
            .contains("2026-07-30T00:00:00Z,2026-07-31T00:00:00Z,2026-07-31T01:00:00Z");
    }

    @Test
    void protectsEverySpreadsheetFormulaPrefixAfterWhitespace() {
        for (String value : List.of("=x", " +x", "\t-x", "\r\n@x")) {
            assertThat(SettlementReportCsvWriter.safeText(value)).isEqualTo("'" + value);
        }
        assertThat(SettlementReportCsvWriter.safeText("SIMULATED")).isEqualTo("SIMULATED");
        assertThat(SettlementReportCsvWriter.safeText(null)).isEmpty();
    }

    private static PaymentSettlementReportRowVo row(String provider) {
        PaymentSettlementReportRowVo row = new PaymentSettlementReportRowVo();
        row.setSettlementDate("2026-07-30"); row.setProviderCode(provider); row.setCurrencyCode("USD");
        row.setBatchCount(2); row.setEventCount(4); row.setPaymentCount(2); row.setRefundCount(1);
        row.setChargebackCount(1); row.setGrossPayment("123.450000"); row.setRefundAmount("10.000000");
        row.setChargebackAmount("20.000000"); row.setTotalFee("3.000000"); row.setNetSettlement("90.450000");
        row.setNegativeNet(false); row.setEarliestPeriodStart("2026-07-30T00:00:00Z");
        row.setLatestPeriodEnd("2026-07-31T00:00:00Z"); row.setLatestCloseTime("2026-07-31T01:00:00Z");
        return row;
    }
}
