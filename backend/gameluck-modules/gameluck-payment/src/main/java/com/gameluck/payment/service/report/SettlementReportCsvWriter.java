package com.gameluck.payment.service.report;

import cn.hutool.core.text.csv.CsvWriter;
import com.gameluck.payment.domain.vo.PaymentSettlementReportRowVo;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Component
public class SettlementReportCsvWriter {
    private static final String[] HEADER = {
        "report_date", "provider_code", "currency_code", "batch_count", "event_count",
        "payment_event_count", "refund_event_count", "chargeback_event_count", "gross_payment",
        "refund_amount", "chargeback_amount", "total_fee", "net_settlement", "negative_net",
        "earliest_period_start", "latest_period_end", "latest_close_time"
    };

    public byte[] write(List<PaymentSettlementReportRowVo> rows) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.writeBytes(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        try (CsvWriter csv = new CsvWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
            csv.writeHeaderLine(HEADER);
            for (PaymentSettlementReportRowVo row : rows) {
                csv.writeLine(row(row));
            }
            csv.flush();
        }
        return output.toByteArray();
    }

    static String safeText(String value) {
        if (value == null) return "";
        int index = 0;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) index++;
        return index < value.length() && "=+-@".indexOf(value.charAt(index)) >= 0 ? "'" + value : value;
    }

    private static String[] row(PaymentSettlementReportRowVo value) {
        return new String[]{
            text(value.getReportDate()), safeText(value.getProviderCode()), safeText(value.getCurrencyCode()),
            text(value.getBatchCount()), text(value.getEventCount()), text(value.getPaymentEventCount()),
            text(value.getRefundEventCount()), text(value.getChargebackEventCount()), value.getGrossPayment(),
            value.getRefundAmount(), value.getChargebackAmount(), value.getTotalFee(), value.getNetSettlement(),
            Boolean.TRUE.equals(value.getNegativeNet()) ? "true" : "false", instant(value.getEarliestPeriodStart()),
            instant(value.getLatestPeriodEnd()), instant(value.getLatestCloseTime())
        };
    }

    private static String text(Object value) { return value == null ? "" : value.toString(); }
    private static String instant(Date value) {
        return value == null ? "" : DateTimeFormatter.ISO_INSTANT.format(value.toInstant());
    }
}
