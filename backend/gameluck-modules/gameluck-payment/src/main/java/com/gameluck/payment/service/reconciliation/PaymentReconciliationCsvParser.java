package com.gameluck.payment.service.reconciliation;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.text.csv.CsvReadConfig;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public final class PaymentReconciliationCsvParser {

    public static final long MAX_BYTES = 10L * 1024 * 1024;
    public static final int MAX_DATA_ROWS = 50_000;

    private static final List<String> HEADERS = List.of(
        "provider_record_id", "event_type", "provider_session_no", "purchase_order_no",
        "pay_currency_code", "pay_amount", "occurred_time");
    private static final Set<String> EVENT_TYPES = Set.of(
        "PAYMENT_SUCCEEDED", "PAYMENT_FAILED", "PAYMENT_CANCELLED", "REFUND_SUCCEEDED",
        "CHARGEBACK_CREATED");

    private final ObjectMapper objectMapper;

    public PaymentReconciliationCsvParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public ReconciliationParseResult parse(InputStream input, long declaredSize) {
        Objects.requireNonNull(input, "input");
        if (declaredSize > MAX_BYTES) {
            return fileError("FILE_TOO_LARGE", null);
        }

        MessageDigest digest = sha256();
        BoundedInputStream bounded = new BoundedInputStream(input, MAX_BYTES);
        DigestInputStream digestInput = new DigestInputStream(bounded, digest);
        InputStreamReader decoded = new InputStreamReader(digestInput, StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT));
        CsvReadConfig config = CsvReadConfig.defaultConfig()
            .setContainsHeader(false)
            .setSkipEmptyRows(true)
            .setErrorOnDifferentFieldCount(false)
            .setTrimField(false);
        List<ReconciliationParsedLine> lines = new ArrayList<>();
        Set<String> providerIds = new HashSet<>();
        boolean rowLimitExceeded = false;

        try (CsvReader reader = new CsvReader(decoded, config)) {
            java.util.Iterator<CsvRow> rows = reader.iterator();
            if (!rows.hasNext() || !validHeader(rows.next())) {
                drain(decoded);
                return fileError("INVALID_HEADER", digestHex(digest));
            }
            while (rows.hasNext()) {
                CsvRow row = rows.next();
                if (lines.size() >= MAX_DATA_ROWS) {
                    rowLimitExceeded = true;
                    continue;
                }
                lines.add(parseRow(row, providerIds));
            }
            // CsvReader may finish before its Reader consumes the final buffered bytes.
            drain(decoded);
            return result(digestHex(digest), lines, rowLimitExceeded ? "ROW_LIMIT_EXCEEDED" : null);
        } catch (RuntimeException | IOException exception) {
            if (hasCause(exception, SizeLimitExceededException.class)) {
                return fileError("FILE_TOO_LARGE", null);
            }
            if (hasCause(exception, CharacterCodingException.class)) {
                try {
                    drain(digestInput);
                    return fileError("INVALID_UTF8", digestHex(digest));
                } catch (IOException drainException) {
                    if (hasCause(drainException, SizeLimitExceededException.class)) {
                        return fileError("FILE_TOO_LARGE", null);
                    }
                    return fileError("CSV_READ_ERROR", null);
                }
            }
            if (exception instanceof IOException
                || (exception instanceof IORuntimeException && hasCause(exception, IOException.class))) {
                return fileError("CSV_READ_ERROR", null);
            }
            throw new IllegalStateException("Unable to parse reconciliation CSV", exception);
        }
    }

    private ReconciliationParsedLine parseRow(CsvRow row, Set<String> providerIds) {
        List<String> fields = row.getRawList();
        String evidence = evidence(fields);
        String providerId = fields.isEmpty() ? "" : fields.get(0).trim();
        boolean duplicateProviderId = !providerId.isBlank() && !providerIds.add(providerId);
        if (fields.size() != HEADERS.size()) {
            return invalid(row, fields, evidence, "FIELD_COUNT");
        }

        String eventType = fields.get(1).trim();
        String sessionNo = fields.get(2).trim();
        String orderNo = fields.get(3).trim();
        String currency = fields.get(4).trim().toUpperCase(Locale.ROOT);
        BigDecimal amount = null;
        Instant occurredTime = null;
        String error = null;

        if (!EVENT_TYPES.contains(eventType)) {
            error = "UNKNOWN_EVENT_TYPE";
        } else if (providerId.isBlank() || sessionNo.isBlank() || orderNo.isBlank()) {
            error = "BLANK_IDENTITY";
        } else if (!validCurrency(currency)) {
            error = "INVALID_CURRENCY";
        } else {
            try {
                amount = new BigDecimal(fields.get(5)).setScale(6, RoundingMode.UNNECESSARY);
                if (amount.precision() > 20) {
                    error = "AMOUNT_OUT_OF_RANGE";
                }
            } catch (NumberFormatException exception) {
                error = "INVALID_DECIMAL";
            } catch (ArithmeticException exception) {
                error = "AMOUNT_SCALE_OVERFLOW";
            }
        }
        if (error == null) {
            try {
                occurredTime = OffsetDateTime.parse(fields.get(6)).toInstant();
            } catch (DateTimeParseException exception) {
                error = "INVALID_TIMESTAMP";
            }
        }
        if (error == null && duplicateProviderId) {
            error = "DUPLICATE_PROVIDER_RECORD_ID";
        }

        return new ReconciliationParsedLine(row.getOriginalLineNumber(), providerId, eventType, sessionNo,
            orderNo, currency, amount, occurredTime, evidence,
            error == null ? ReconciliationParsedLine.Status.VALID : ReconciliationParsedLine.Status.INVALID,
            error);
    }

    private ReconciliationParsedLine invalid(CsvRow row, List<String> fields, String evidence, String error) {
        return new ReconciliationParsedLine(row.getOriginalLineNumber(), field(fields, 0), field(fields, 1),
            field(fields, 2), field(fields, 3), normalizeCurrency(field(fields, 4)), null, null, evidence,
            ReconciliationParsedLine.Status.INVALID, error);
    }

    private String evidence(List<String> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize CSV source fields", exception);
        }
    }

    private static boolean validHeader(CsvRow row) {
        List<String> values = new ArrayList<>(row.getRawList());
        if (!values.isEmpty() && values.get(0).startsWith("\uFEFF")) {
            values.set(0, values.get(0).substring(1));
        }
        return values.equals(HEADERS);
    }

    private static boolean validCurrency(String currency) {
        return currency.length() == 3 && currency.chars().allMatch(character -> character >= 'A' && character <= 'Z');
    }

    private static String normalizeCurrency(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String field(List<String> fields, int index) {
        return index < fields.size() ? fields.get(index).trim() : null;
    }

    private static void drain(InputStreamReader reader) throws IOException {
        char[] buffer = new char[4096];
        while (reader.read(buffer) != -1) {
            // Drain through the bounded, digesting and strict-decoding stream.
        }
    }

    private static void drain(InputStream input) throws IOException {
        byte[] buffer = new byte[4096];
        while (input.read(buffer) != -1) {
            // Drain exact source bytes through the digesting stream.
        }
    }

    private static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause)) {
                return true;
            }
        }
        return false;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String digestHex(MessageDigest digest) {
        return HexFormat.of().formatHex(digest.digest());
    }

    private static ReconciliationParseResult result(String digest, List<ReconciliationParsedLine> lines,
                                                     String fileError) {
        long valid = lines.stream().filter(line -> line.status() == ReconciliationParsedLine.Status.VALID).count();
        return new ReconciliationParseResult(digest, lines.size(), valid, lines.size() - valid, lines, fileError);
    }

    private static ReconciliationParseResult fileError(String error, String digest) {
        return new ReconciliationParseResult(digest, 0, 0, 0, List.of(), error);
    }

    private static final class BoundedInputStream extends InputStream {
        private final InputStream delegate;
        private final long limit;
        private long count;

        private BoundedInputStream(InputStream delegate, long limit) {
            this.delegate = delegate;
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value == -1) {
                return -1;
            }
            count++;
            if (count > limit) {
                throw new SizeLimitExceededException();
            }
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            int allowed = (int) Math.min(length, limit - count + 1);
            int read = delegate.read(bytes, offset, allowed);
            if (read == -1) {
                return -1;
            }
            count += read;
            if (count > limit) {
                throw new SizeLimitExceededException();
            }
            return read;
        }
    }

    private static final class SizeLimitExceededException extends IOException {
    }
}
