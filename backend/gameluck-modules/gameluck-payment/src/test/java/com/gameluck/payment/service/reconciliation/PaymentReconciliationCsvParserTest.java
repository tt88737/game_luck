package com.gameluck.payment.service.reconciliation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("local")
class PaymentReconciliationCsvParserTest {

    private static final String HEADER = "provider_record_id,event_type,provider_session_no,purchase_order_no,"
        + "pay_currency_code,pay_amount,occurred_time\n";
    private static final String VALID_ROW = "pr-1,PAYMENT_SUCCEEDED,session-1,order-1, usd ,12.340000,2026-07-28T10:15:30+08:00\n";
    private final PaymentReconciliationCsvParser parser =
        new PaymentReconciliationCsvParser(new ObjectMapper());

    @Test
    void parsesExactHeaderBomCrLfQuotedValuesEscapedQuotesBlankLinesAndTrailingFields() throws Exception {
        String csv = "\uFEFF" + HEADER.replace("\n", "\r\n")
            + "\r\n\"pr,1\",PAYMENT_SUCCEEDED,\"session \"\"one\"\"\",order-1, usd ,12.34,2026-07-28T10:15:30+08:00\r\n"
            + "pr-2,PAYMENT_FAILED,session-2,order-2,EUR,0,\r\n";

        ReconciliationParseResult result = parse(csv);

        assertThat(result.fileErrorCode()).isNull();
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.validCount()).isEqualTo(1);
        assertThat(result.invalidCount()).isEqualTo(1);
        assertThat(result.lines()).hasSize(2);
        ReconciliationParsedLine first = result.lines().get(0);
        assertThat(first.sourceLineNumber()).isEqualTo(2);
        assertThat(first.providerRecordId()).isEqualTo("pr,1");
        assertThat(first.providerSessionNo()).isEqualTo("session \"one\"");
        assertThat(first.currency()).isEqualTo("USD");
        assertThat(first.amount()).isEqualByComparingTo("12.340000");
        assertThat(first.occurredTime()).hasToString("2026-07-28T02:15:30Z");
        assertThat(first.sourceFieldsJson()).isEqualTo(
            "[\"pr,1\",\"PAYMENT_SUCCEEDED\",\"session \\\"one\\\"\",\"order-1\",\" usd \",\"12.34\",\"2026-07-28T10:15:30+08:00\"]");
        assertThat(result.lines().get(1).sourceFieldsJson()).endsWith(",\"\"]");
        assertThat(result.lines().get(1).parseErrorCode()).isEqualTo("INVALID_TIMESTAMP");
    }

    @Test
    void hashesTheExactOriginalBytesIncludingBomAndLineEndings() throws Exception {
        byte[] bytes = ("\uFEFF" + HEADER.replace("\n", "\r\n") + VALID_ROW.replace("\n", "\r\n"))
            .getBytes(StandardCharsets.UTF_8);

        ReconciliationParseResult result = parser.parse(new ByteArrayInputStream(bytes), bytes.length);

        assertThat(result.sha256Digest()).isEqualTo(HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(bytes)));
    }

    @Test
    void rejectsInvalidUtf8() {
        byte[] prefix = HEADER.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = java.util.Arrays.copyOf(prefix, prefix.length + 2);
        bytes[prefix.length] = (byte) 0xC3;
        bytes[prefix.length + 1] = (byte) 0x28;

        ReconciliationParseResult result = parser.parse(new ByteArrayInputStream(bytes), bytes.length);

        assertThat(result.fileErrorCode()).isEqualTo("INVALID_UTF8");
        assertThat(result.lines()).isEmpty();
    }

    @Test
    void returnsCsvReadErrorWhenDrainingAfterAnInvalidHeaderFails() {
        byte[] available = ("bad_header\n" + "x".repeat(9_000)).getBytes(StandardCharsets.UTF_8);
        InputStream input = new InputStream() {
            private int position;

            @Override
            public int read() throws IOException {
                if (position < available.length) {
                    return available[position++] & 0xff;
                }
                throw new IOException("simulated source failure");
            }

            @Override
            public int read(byte[] bytes, int offset, int length) throws IOException {
                if (position < available.length) {
                    int count = Math.min(length, available.length - position);
                    System.arraycopy(available, position, bytes, offset, count);
                    position += count;
                    return count;
                }
                throw new IOException("simulated source failure");
            }
        };

        ReconciliationParseResult result = parser.parse(input, -1);

        assertThat(result.fileErrorCode()).isEqualTo("CSV_READ_ERROR");
        assertThat(result.lines()).isEmpty();
    }

    @Test
    void doesNotMisclassifyEvidenceSerializationFailureAsCsvReadError() {
        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("simulated serialization failure") { };
            }
        };
        PaymentReconciliationCsvParser failingParser = new PaymentReconciliationCsvParser(failingMapper);
        byte[] bytes = (HEADER + VALID_ROW).getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> failingParser.parse(new ByteArrayInputStream(bytes), bytes.length))
            .isInstanceOf(IllegalStateException.class)
            .hasRootCauseMessage("simulated serialization failure");
    }

    @Test
    void rejectsMissingDuplicateAndOutOfOrderHeaders() {
        assertThat(parse("provider_record_id,event_type\n").fileErrorCode()).isEqualTo("INVALID_HEADER");
        assertThat(parse(HEADER.replace("purchase_order_no", "provider_record_id")).fileErrorCode())
            .isEqualTo("INVALID_HEADER");
        assertThat(parse(HEADER.replace("provider_record_id,event_type", "event_type,provider_record_id"))
            .fileErrorCode()).isEqualTo("INVALID_HEADER");
    }

    @Test
    void reportsStableValidationCodesAndContinuesParsing() {
        String csv = HEADER
            + "a,NOT_REAL,s,o,USD,1,2026-07-28T00:00:00Z\n"
            + " ,PAYMENT_SUCCEEDED,s,o,USD,1,2026-07-28T00:00:00Z\n"
            + "c,PAYMENT_SUCCEEDED,s,o,US,1,2026-07-28T00:00:00Z\n"
            + "d,PAYMENT_SUCCEEDED,s,o,USD,nope,2026-07-28T00:00:00Z\n"
            + "e,PAYMENT_SUCCEEDED,s,o,USD,1.0000001,2026-07-28T00:00:00Z\n"
            + "f,PAYMENT_SUCCEEDED,s,o,USD,1,2026-07-28T00:00:00\n"
            + "g,PAYMENT_SUCCEEDED,s,o,USD,1,2026-07-28T00:00:00Z\n";

        ReconciliationParseResult result = parse(csv);

        assertThat(result.lines()).extracting(ReconciliationParsedLine::parseErrorCode).containsExactly(
            "UNKNOWN_EVENT_TYPE", "BLANK_IDENTITY", "INVALID_CURRENCY", "INVALID_DECIMAL",
            "AMOUNT_SCALE_OVERFLOW", "INVALID_TIMESTAMP", null);
        assertThat(result.totalCount()).isEqualTo(7);
        assertThat(result.validCount()).isEqualTo(1);
        assertThat(result.invalidCount()).isEqualTo(6);
        assertThat(result.lines().get(0).status()).isEqualTo(ReconciliationParsedLine.Status.INVALID);
        assertThat(result.lines().get(6).status()).isEqualTo(ReconciliationParsedLine.Status.VALID);
    }

    @Test
    void rejectsDuplicateProviderRecordIdsOnTheLaterLine() {
        ReconciliationParseResult result = parse(HEADER + VALID_ROW + VALID_ROW);

        assertThat(result.lines()).extracting(ReconciliationParsedLine::parseErrorCode)
            .containsExactly(null, "DUPLICATE_PROVIDER_RECORD_ID");
        assertThat(result.validCount()).isEqualTo(1);
        assertThat(result.invalidCount()).isEqualTo(1);
    }

    @Test
    void tracksProviderIdEvenWhenItsFirstRowHasAnotherValidationError() {
        String csv = HEADER
            + "same,NOT_REAL,s,o,USD,1,2026-07-28T00:00:00Z\n"
            + "same,PAYMENT_SUCCEEDED,s,o,USD,1,2026-07-28T00:00:00Z\n";

        ReconciliationParseResult result = parse(csv);

        assertThat(result.lines()).extracting(ReconciliationParsedLine::parseErrorCode)
            .containsExactly("UNKNOWN_EVENT_TYPE", "DUPLICATE_PROVIDER_RECORD_ID");
    }

    @Test
    void tracksProviderIdBeforeRejectingTheFirstRowFieldCount() {
        String csv = HEADER
            + "same,PAYMENT_SUCCEEDED,s,o,USD,1\n"
            + "same,PAYMENT_SUCCEEDED,s,o,USD,1,2026-07-28T00:00:00Z\n";

        ReconciliationParseResult result = parse(csv);

        assertThat(result.lines()).extracting(ReconciliationParsedLine::parseErrorCode)
            .containsExactly("FIELD_COUNT", "DUPLICATE_PROVIDER_RECORD_ID");
    }

    @Test
    void rejectsAmountsThatCannotFitDecimalTwentySix() {
        ReconciliationParseResult result = parse(HEADER
            + "a,PAYMENT_SUCCEEDED,s,o,USD,123456789012345.000000,2026-07-28T00:00:00Z\n");

        assertThat(result.lines()).extracting(ReconciliationParsedLine::parseErrorCode)
            .containsExactly("AMOUNT_OUT_OF_RANGE");
    }

    @Test
    void rejectsTheFiftyThousandAndFirstDataRowWithoutRetainingIt() {
        String row = "x,PAYMENT_SUCCEEDED,s,o,USD,1,2026-07-28T00:00:00Z\n";
        StringBuilder csv = new StringBuilder(HEADER.length() + row.length() * 50_001);
        csv.append(HEADER);
        for (int i = 0; i < 50_001; i++) {
            csv.append(row.replace("x,", "x" + i + ","));
        }

        ReconciliationParseResult result = parse(csv.toString());

        assertThat(result.fileErrorCode()).isEqualTo("ROW_LIMIT_EXCEEDED");
        assertThat(result.sha256Digest()).isEqualTo(HexFormat.of().formatHex(sha256(csv.toString().getBytes(StandardCharsets.UTF_8))));
        assertThat(result.lines()).hasSize(50_000);
        assertThat(result.totalCount()).isEqualTo(50_000);
    }

    @Test
    void fileSizeErrorWinsWhenInputExceedsBothRowAndByteLimits() {
        String row = "x,PAYMENT_SUCCEEDED,s,o,USD,1,2026-07-28T00:00:00Z\n";
        StringBuilder prefix = new StringBuilder(HEADER);
        for (int i = 0; i < 50_001; i++) {
            prefix.append(row);
        }
        byte[] prefixBytes = prefix.toString().getBytes(StandardCharsets.UTF_8);
        TrackingPrefixInputStream input = new TrackingPrefixInputStream(prefixBytes,
            PaymentReconciliationCsvParser.MAX_BYTES + 100);

        ReconciliationParseResult result = parser.parse(input, -1);

        assertThat(result.fileErrorCode()).isEqualTo("FILE_TOO_LARGE");
        assertThat(result.sha256Digest()).isNull();
        assertThat(result.lines()).isEmpty();
        assertThat(input.bytesRead).isEqualTo(PaymentReconciliationCsvParser.MAX_BYTES + 1);
    }

    @Test
    void rejectsDeclaredSizeAboveTenMibWithoutReading() {
        TrackingInputStream input = new TrackingInputStream(PaymentReconciliationCsvParser.MAX_BYTES + 100);

        ReconciliationParseResult result = parser.parse(input, PaymentReconciliationCsvParser.MAX_BYTES + 1);

        assertThat(result.fileErrorCode()).isEqualTo("FILE_TOO_LARGE");
        assertThat(input.bytesRead).isZero();
    }

    @Test
    void stopsReadingAtTheFirstByteBeyondTenMibAndRetainsNoRows() {
        TrackingInputStream input = new TrackingInputStream(PaymentReconciliationCsvParser.MAX_BYTES + 100);

        ReconciliationParseResult result = parser.parse(input, -1);

        assertThat(result.fileErrorCode()).isEqualTo("FILE_TOO_LARGE");
        assertThat(input.bytesRead).isEqualTo(PaymentReconciliationCsvParser.MAX_BYTES + 1);
        assertThat(result.lines()).isEmpty();
    }

    private ReconciliationParseResult parse(String csv) {
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return parser.parse(new ByteArrayInputStream(bytes), bytes.length);
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class TrackingInputStream extends InputStream {
        private final long size;
        private long bytesRead;

        private TrackingInputStream(long size) {
            this.size = size;
        }

        @Override
        public int read() {
            if (bytesRead >= size) {
                return -1;
            }
            bytesRead++;
            return 'x';
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (bytesRead >= size) {
                return -1;
            }
            int count = (int) Math.min(length, size - bytesRead);
            java.util.Arrays.fill(bytes, offset, offset + count, (byte) 'x');
            bytesRead += count;
            return count;
        }
    }

    private static final class TrackingPrefixInputStream extends InputStream {
        private final byte[] prefix;
        private final long size;
        private long bytesRead;

        private TrackingPrefixInputStream(byte[] prefix, long size) {
            this.prefix = prefix;
            this.size = size;
        }

        @Override
        public int read() {
            if (bytesRead >= size) {
                return -1;
            }
            int value = bytesRead < prefix.length ? prefix[(int) bytesRead] & 0xff : 'x';
            bytesRead++;
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) {
            if (bytesRead >= size) {
                return -1;
            }
            int count = (int) Math.min(length, size - bytesRead);
            for (int i = 0; i < count; i++) {
                long position = bytesRead + i;
                bytes[offset + i] = position < prefix.length ? prefix[(int) position] : (byte) 'x';
            }
            bytesRead += count;
            return count;
        }
    }
}
