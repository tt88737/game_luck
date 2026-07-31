package com.gameluck.payment.service.report;

import cn.hutool.core.text.csv.CsvWriter;
import com.gameluck.payment.domain.vo.PaymentSettlementReportRowVo;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SettlementReportCsvWriter {
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final String[] HEADER = {
        "report_date", "provider_code", "currency_code", "batch_count", "event_count",
        "payment_event_count", "refund_event_count", "chargeback_event_count", "gross_payment",
        "refund_amount", "chargeback_amount", "total_fee", "net_settlement", "negative_net",
        "earliest_period_start", "latest_period_end", "latest_close_time"
    };

    public byte[] write(List<PaymentSettlementReportRowVo> rows) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.writeBytes(UTF8_BOM);
        CsvWriter writer = new CsvWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        writer.writeHeaderLine(HEADER);
        for (PaymentSettlementReportRowVo row : rows) writer.writeLine(cells(row));
        writer.flush();
        return output.toByteArray();
    }

    static String safeText(String value) {
        if (value == null) return "";
        int index = 0;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) index++;
        return index < value.length() && "=+-@".indexOf(value.charAt(index)) >= 0 ? "'" + value : value;
    }

    private String[] cells(PaymentSettlementReportRowVo row) {
        return new String[]{
            safeText(row.getSettlementDate()), safeText(row.getProviderCode()), safeText(row.getCurrencyCode()),
            integer(row.getBatchCount()), integer(row.getEventCount()), integer(row.getPaymentCount()),
            integer(row.getRefundCount()), integer(row.getChargebackCount()), row.getGrossPayment(),
            row.getRefundAmount(), row.getChargebackAmount(), row.getTotalFee(), row.getNetSettlement(),
            Boolean.TRUE.equals(row.getNegativeNet()) ? "true" : "false", safeText(row.getEarliestPeriodStart()),
            safeText(row.getLatestPeriodEnd()), safeText(row.getLatestCloseTime())
        };
    }

    private String integer(Integer value) {
        return value == null ? "" : value.toString();
    }
}
