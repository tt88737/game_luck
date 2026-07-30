package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.payment.enums.PaymentReconciliationBatchStatus;
import com.gameluck.payment.enums.PaymentReconciliationIssueStatus;
import com.gameluck.payment.enums.PaymentReconciliationIssueType;
import com.gameluck.payment.enums.PaymentReconciliationLineStatus;
import com.gameluck.payment.enums.PaymentReconciliationResolutionType;
import com.gameluck.payment.mapper.PaymentReconciliationActionLogMapper;
import com.gameluck.payment.mapper.PaymentReconciliationBatchMapper;
import com.gameluck.payment.mapper.PaymentReconciliationIssueMapper;
import com.gameluck.payment.mapper.PaymentReconciliationLineMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("local")
class PaymentReconciliationPersistenceContractTest {

    @Test
    void exposesExactReconciliationEnums() {
        assertThat(names(PaymentReconciliationBatchStatus.values())).containsExactly(
            "UPLOADED", "VALIDATED", "RECONCILING", "COMPLETED", "FAILED");
        assertThat(names(PaymentReconciliationLineStatus.values())).containsExactly(
            "VALID", "INVALID", "MATCHED", "ISSUE");
        assertThat(names(PaymentReconciliationIssueStatus.values())).containsExactly(
            "OPEN", "RESOLVED", "IGNORED");
        assertThat(names(PaymentReconciliationIssueType.values())).containsExactly(
            "PLATFORM_RECORD_MISSING", "PROVIDER_RECORD_MISSING", "ORDER_IDENTITY_MISMATCH",
            "AMOUNT_MISMATCH", "CURRENCY_MISMATCH", "EVENT_MISSING", "STATUS_MISMATCH",
            "DUPLICATE_PROVIDER_RECORD", "UNSUPPORTED_RECORD");
        assertThat(names(PaymentReconciliationResolutionType.values())).containsExactly(
            "PLATFORM_CONFIRMED", "PROVIDER_CONFIRMED", "EXPECTED_DIFFERENCE",
            "DUPLICATE_CONFIRMED", "OTHER");
    }

    @Test
    void entitiesExposeApprovedPersistenceShape() {
        assertFields(PaymentReconciliationBatch.class, fields(
            "id", Long.class, "tenantId", String.class, "providerCode", String.class,
            "statementDate", Date.class, "originalFileName", String.class, "fileDigest", String.class,
            "totalCount", Integer.class, "validCount", Integer.class, "invalidCount", Integer.class,
            "matchedCount", Integer.class, "discrepancyCount", Integer.class, "status", String.class,
            "failureReason", String.class, "creatorId", Long.class, "creatorName", String.class,
            "version", Integer.class, "createTime", Date.class, "updateTime", Date.class));
        assertFields(PaymentReconciliationLine.class, fields(
            "id", Long.class, "tenantId", String.class, "batchId", Long.class,
            "sourceRowNumber", Long.class, "providerRecordId", String.class, "eventType", String.class,
            "providerSessionNo", String.class, "purchaseOrderNo", String.class,
            "currencyCode", String.class, "amount", BigDecimal.class, "occurredTime", Date.class,
            "status", String.class, "parseError", String.class, "rawFieldsJson", String.class,
            "createTime", Date.class));
        assertFields(PaymentReconciliationIssue.class, fields(
            "id", Long.class, "tenantId", String.class, "batchId", Long.class, "lineId", Long.class,
            "issueType", String.class, "status", String.class, "paymentSessionId", Long.class,
            "sessionNo", String.class, "purchaseOrderId", Long.class, "purchaseOrderNo", String.class,
            "webhookEventId", Long.class, "reversalId", Long.class,
            "providerEventType", String.class, "platformEventType", String.class,
            "providerCurrencyCode", String.class, "platformCurrencyCode", String.class,
            "providerAmount", BigDecimal.class, "platformAmount", BigDecimal.class,
            "providerStatus", String.class, "platformStatus", String.class,
            "diagnosticSnapshotJson", String.class, "resolutionType", String.class,
            "resolutionRemark", String.class, "resolvedBy", Long.class, "resolvedTime", Date.class,
            "version", Integer.class, "createTime", Date.class, "updateTime", Date.class));
        assertFields(PaymentReconciliationActionLog.class, fields(
            "id", Long.class, "tenantId", String.class, "batchId", Long.class, "issueId", Long.class,
            "actionType", String.class, "beforeStatus", String.class, "afterStatus", String.class,
            "operatorId", Long.class, "operatorName", String.class, "remark", String.class,
            "createTime", Date.class));
        assertThat(allFieldNames()).noneMatch(name -> name.matches(".*(secret|credential|card|instrument).*"));
    }

    @Test
    void mappersExposeTenantScopedReadAndGuardedWriteContracts() throws Exception {
        assertInsert(PaymentReconciliationBatchMapper.class, PaymentReconciliationBatch.class,
            "tenant_id", "#{entity.tenantId}", "file_digest", "#{entity.fileDigest}", "version");
        assertInsert(PaymentReconciliationBatchMapper.class, PaymentReconciliationBatch.class,
            "coalesce(#{entity.totalcount},0)", "coalesce(#{entity.validcount},0)",
            "coalesce(#{entity.invalidcount},0)", "coalesce(#{entity.matchedcount},0)",
            "coalesce(#{entity.discrepancycount},0)", "coalesce(#{entity.version},0)",
            "coalesce(#{entity.createtime},current_timestamp)");
        assertSelect(PaymentReconciliationBatchMapper.class, "selectPageByTenant",
            new Class<?>[]{Page.class, String.class, com.gameluck.payment.domain.bo.PaymentReconciliationBatchBo.class},
            "tenant_id = #{tenantId}", "order by create_time desc,id desc");
        assertSelect(PaymentReconciliationBatchMapper.class, "selectByTenantAndId",
            new Class<?>[]{String.class, Long.class}, "tenant_id = #{tenantId}", "id = #{id}");
        assertSelect(PaymentReconciliationBatchMapper.class, "selectByDigest",
            new Class<?>[]{String.class, String.class, String.class}, "tenant_id = #{tenantId}",
            "provider_code = #{providerCode}", "file_digest = #{fileDigest}");
        assertUpdate(PaymentReconciliationBatchMapper.class, "transitionStatus",
            new Class<?>[]{String.class, Long.class, String.class, String.class, Date.class},
            "tenant_id = #{tenantId}", "id = #{id}", "status = #{expectedStatus}",
            "status = #{nextStatus}", "version = version + 1");
        assertSelect(PaymentReconciliationLineMapper.class, "selectPageByBatch",
            new Class<?>[]{Page.class, String.class, Long.class, String.class}, "tenant_id = #{tenantId}",
            "batch_id = #{batchId}", "order by source_row_number,id");
        assertInsert(PaymentReconciliationLineMapper.class, PaymentReconciliationLine.class,
            "tenant_id", "#{entity.tenantId}", "source_row_number", "raw_fields_json",
            "#{entity.rawFieldsJson}", "coalesce(#{entity.createtime},current_timestamp)");
        assertSelect(PaymentReconciliationIssueMapper.class, "selectPageByBatch",
            new Class<?>[]{Page.class, String.class, Long.class}, "tenant_id = #{tenantId}",
            "batch_id = #{batchId}", "order by create_time desc,id desc");
        assertSelect(PaymentReconciliationIssueMapper.class, "selectByTenantAndId",
            new Class<?>[]{String.class, Long.class}, "tenant_id = #{tenantId}", "id = #{id}");
        assertInsert(PaymentReconciliationIssueMapper.class, PaymentReconciliationIssue.class,
            "tenant_id", "#{entity.tenantId}", "diagnostic_snapshot_json",
            "#{entity.diagnosticSnapshotJson}", "coalesce(#{entity.version},0)",
            "coalesce(#{entity.createtime},current_timestamp)");
        assertUpdate(PaymentReconciliationIssueMapper.class, "resolveOpenIssue",
            new Class<?>[]{String.class, Long.class, Integer.class, String.class, String.class,
                String.class, Long.class, Date.class},
            "tenant_id = #{tenantId}", "id = #{id}", "status = 'OPEN'", "version = #{expectedVersion}",
            "status = #{nextStatus}", "resolution_remark = #{remark}", "version = version + 1");
        assertSelect(PaymentReconciliationActionLogMapper.class, "selectByBatch",
            new Class<?>[]{String.class, Long.class}, "tenant_id = #{tenantId}",
            "batch_id = #{batchId}", "order by create_time,id");
        assertInsert(PaymentReconciliationActionLogMapper.class, PaymentReconciliationActionLog.class,
            "tenant_id", "#{entity.tenantId}", "action_type", "before_status", "after_status",
            "operator_id", "remark", "create_time", "coalesce(#{entity.createtime},current_timestamp)");
    }

    @Test
    void mappersExposeOnlyExplicitPersistenceOperations() {
        assertMapperMethodsInclude(PaymentReconciliationBatchMapper.class,
            "insert", "selectPageByTenant", "selectByTenantAndId", "selectByDigest", "transitionStatus",
            "finalizeValidation", "markFailed", "acquireExecution", "completeExecution", "markExecutionFailed");
        assertMapperMethodsInclude(PaymentReconciliationLineMapper.class,
            "insert", "insertBatch", "selectPageByBatch", "selectValidChunk", "concludeValidLine", "concludeValidLines");
        assertMapperMethodsInclude(PaymentReconciliationIssueMapper.class,
            "insert", "insertBatch", "selectPageByBatch", "selectByTenantAndId", "resolveOpenIssue");
        assertMapperMethodsInclude(PaymentReconciliationActionLogMapper.class, "insert", "selectByBatch");
        for (Class<?> mapper : new Class<?>[]{PaymentReconciliationBatchMapper.class,
            PaymentReconciliationLineMapper.class, PaymentReconciliationIssueMapper.class,
            PaymentReconciliationActionLogMapper.class}) {
            assertThat(mapper.getInterfaces()).isEmpty();
            assertThat(Arrays.stream(mapper.getMethods()).map(Method::getName))
                .noneMatch(name -> name.matches("updateById|update|deleteById|delete|deleteBatchIds"));
        }
    }

    @Test
    void walletSqlDefinesIdempotentTenantScopedReconciliationTables() throws Exception {
        String sql = Files.readString(findWalletSql());
        String normalized = normalize(sql);
        String batches = tableDefinition(sql, "gl_payment_reconciliation_batch");
        assertThat(batches).contains(
            "id bigint not null", "tenant_id varchar(20) not null default '000000'",
            "provider_code varchar(64) not null", "statement_date date not null",
            "original_file_name varchar(255) not null", "file_digest varchar(64) not null",
            "total_count int not null default 0", "valid_count int not null default 0",
            "invalid_count int not null default 0", "matched_count int not null default 0",
            "discrepancy_count int not null default 0", "status varchar(32) not null",
            "failure_reason varchar(500) default null", "creator_id bigint not null",
            "creator_name varchar(100) not null", "version int not null default 0",
            "create_time datetime not null default current_timestamp", "update_time datetime default null",
            "unique key uk_gl_payment_reconciliation_batch_01 (tenant_id, provider_code, file_digest)",
            "key idx_gl_payment_reconciliation_batch_01 (tenant_id, status, statement_date)",
            "key idx_gl_payment_reconciliation_batch_02 (tenant_id, provider_code, statement_date, id)",
            "key idx_gl_payment_reconciliation_batch_03 (tenant_id, create_time, id)");
        String lines = tableDefinition(sql, "gl_payment_reconciliation_line");
        assertThat(lines).contains(
            "id bigint not null", "tenant_id varchar(20) not null default '000000'",
            "batch_id bigint not null", "source_row_number bigint not null",
            "provider_record_id varchar(128) default null", "event_type varchar(32) default null",
            "provider_session_no varchar(128) default null", "purchase_order_no varchar(64) default null",
            "currency_code varchar(32) default null", "amount decimal(20,6) default null",
            "occurred_time datetime default null", "status varchar(32) not null",
            "parse_error varchar(500) default null", "raw_fields_json longtext not null",
            "create_time datetime not null default current_timestamp",
            "unique key uk_gl_payment_reconciliation_line_01 (tenant_id, batch_id, source_row_number)",
            "key idx_gl_payment_reconciliation_line_02 (tenant_id, provider_record_id)",
            "key idx_gl_payment_reconciliation_line_03 (tenant_id, purchase_order_no)");
        assertThat(lines).doesNotContain("key idx_gl_payment_reconciliation_line_01");
        assertThat(lines).doesNotContain("update_time", "on update");
        String issues = tableDefinition(sql, "gl_payment_reconciliation_issue");
        assertThat(issues).contains(
            "id bigint not null", "tenant_id varchar(20) not null default '000000'",
            "batch_id bigint not null", "line_id bigint default null",
            "issue_type varchar(64) not null", "status varchar(32) not null",
            "payment_session_id bigint default null", "session_no varchar(64) default null",
            "purchase_order_id bigint default null", "purchase_order_no varchar(64) default null",
            "webhook_event_id bigint default null", "reversal_id bigint default null",
            "provider_event_type varchar(32) default null", "platform_event_type varchar(32) default null",
            "provider_currency_code varchar(32) default null", "platform_currency_code varchar(32) default null",
            "provider_amount decimal(20,6) default null", "platform_amount decimal(20,6) default null",
            "provider_status varchar(32) default null", "platform_status varchar(32) default null",
            "diagnostic_snapshot_json longtext not null", "resolution_type varchar(32) default null",
            "resolution_remark varchar(500) default null", "resolved_by bigint default null",
            "resolved_time datetime default null", "version int not null default 0",
            "create_time datetime not null default current_timestamp", "update_time datetime default null",
            "unique key uk_gl_payment_reconciliation_issue_01 (tenant_id, batch_id, line_id)",
            "key idx_gl_payment_reconciliation_issue_01 (tenant_id, batch_id, status, create_time)",
            "key idx_gl_payment_reconciliation_issue_02 (tenant_id, line_id)",
            "key idx_gl_payment_reconciliation_issue_03 (tenant_id, purchase_order_no)",
            "key idx_gl_payment_reconciliation_issue_04 (tenant_id, session_no)",
            "key idx_gl_payment_reconciliation_issue_05 (tenant_id, batch_id, create_time, id)");
        String logs = tableDefinition(sql, "gl_payment_reconciliation_action_log");
        assertThat(logs).contains(
            "id bigint not null", "tenant_id varchar(20) not null default '000000'",
            "batch_id bigint not null", "issue_id bigint default null", "action_type varchar(32) not null",
            "before_status varchar(32) default null", "after_status varchar(32) default null",
            "operator_id bigint not null", "operator_name varchar(100) not null",
            "remark varchar(500) default null", "create_time datetime not null default current_timestamp",
            "key idx_gl_payment_reconciliation_action_log_01 (tenant_id, batch_id, create_time, id)",
            "key idx_gl_payment_reconciliation_action_log_02 (tenant_id, issue_id, create_time, id)");
        assertThat(logs).doesNotContain("update_time", "on update");
        for (String table : new String[]{"gl_payment_reconciliation_batch", "gl_payment_reconciliation_line",
            "gl_payment_reconciliation_issue", "gl_payment_reconciliation_action_log"}) {
            assertThat(normalized).contains("create table if not exists " + table + " (");
            assertThat(normalized).doesNotContain("drop table " + table, "truncate table " + table);
        }
        assertThat(batches + lines + issues + logs)
            .doesNotContain("foreign key", "secret", "credential", "card_number", "payment_instrument");
    }

    private static String[] names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toArray(String[]::new);
    }

    private static Map<String, Class<?>> fields(Object... namesAndTypes) {
        Map<String, Class<?>> result = new LinkedHashMap<>();
        for (int i = 0; i < namesAndTypes.length; i += 2) result.put((String) namesAndTypes[i], (Class<?>) namesAndTypes[i + 1]);
        return result;
    }

    private static void assertFields(Class<?> type, Map<String, Class<?>> expected) {
        Map<String, Class<?>> actual = new LinkedHashMap<>();
        Arrays.stream(type.getDeclaredFields()).filter(field -> !field.isSynthetic())
            .forEach(field -> actual.put(field.getName(), field.getType()));
        assertThat(actual).containsExactlyEntriesOf(expected);
    }

    private static java.util.stream.Stream<String> allFieldNames() {
        return Arrays.stream(new Class<?>[]{PaymentReconciliationBatch.class, PaymentReconciliationLine.class,
            PaymentReconciliationIssue.class, PaymentReconciliationActionLog.class})
            .flatMap(type -> Arrays.stream(type.getDeclaredFields())).map(Field::getName)
            .map(String::toLowerCase);
    }

    private static void assertSelect(Class<?> mapper, String name, Class<?>[] types, String... fragments) throws Exception {
        assertSql(mapper.getMethod(name, types), Select.class, fragments);
    }

    private static void assertUpdate(Class<?> mapper, String name, Class<?>[] types, String... fragments) throws Exception {
        assertSql(mapper.getMethod(name, types), Update.class, fragments);
    }

    private static void assertInsert(Class<?> mapper, Class<?> entityType, String... fragments) throws Exception {
        Method method = mapper.getMethod("insert", entityType);
        assertSql(method, Insert.class, fragments);
        assertInsertColumnValueOrder(method);
    }

    private static void assertSql(Method method, Class<? extends java.lang.annotation.Annotation> annotation,
                                  String... fragments) {
        String[] value;
        if (annotation == Select.class) value = method.getAnnotation(Select.class).value();
        else if (annotation == Update.class) value = method.getAnnotation(Update.class).value();
        else value = method.getAnnotation(Insert.class).value();
        assertThat(String.join(" ", value).toLowerCase())
            .contains(Arrays.stream(fragments).map(String::toLowerCase).toArray(String[]::new));
    }

    private static void assertMapperMethods(Class<?> mapper, String... expected) {
        assertThat(Arrays.stream(mapper.getDeclaredMethods()).map(Method::getName))
            .containsExactlyInAnyOrder(expected);
    }

    private static void assertMapperMethodsInclude(Class<?> mapper, String... required) {
        assertThat(Arrays.stream(mapper.getDeclaredMethods()).map(Method::getName))
            .contains(required)
            .allMatch(name -> !name.matches("updateById|update|deleteById|delete|deleteBatchIds"));
    }

    private static void assertInsertColumnValueOrder(Method method) {
        String sql = normalize(String.join(" ", method.getAnnotation(Insert.class).value()));
        Matcher matcher = Pattern.compile("insert into \\S+ \\((.*?)\\) values \\((.*)\\)").matcher(sql);
        assertThat(matcher.matches()).as("insert shape for %s", method.getDeclaringClass().getSimpleName()).isTrue();
        String[] columns = matcher.group(1).split(",");
        java.util.List<String> values = splitTopLevelValues(matcher.group(2));
        assertThat(values).hasSameSizeAs(Arrays.asList(columns));
        for (int index = 0; index < columns.length; index++) {
            assertThat(values.get(index)).as("value for column %s", columns[index])
                .contains(("#{entity." + snakeToCamel(columns[index]) + "}").toLowerCase());
        }
    }

    private static java.util.List<String> splitTopLevelValues(String text) {
        java.util.List<String> values = new java.util.ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '(') depth++;
            else if (character == ')') depth--;
            else if (character == ',' && depth == 0) {
                values.add(text.substring(start, index));
                start = index + 1;
            }
        }
        values.add(text.substring(start));
        return values;
    }

    private static String snakeToCamel(String value) {
        StringBuilder result = new StringBuilder();
        boolean uppercase = false;
        for (char character : value.toCharArray()) {
            if (character == '_') uppercase = true;
            else {
                result.append(uppercase ? Character.toUpperCase(character) : character);
                uppercase = false;
            }
        }
        return result.toString();
    }

    private static Path findWalletSql() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("script/sql/gameluck_wallet.sql");
            if (Files.isRegularFile(candidate)) return candidate;
            current = current.getParent();
        }
        throw new AssertionError("script/sql/gameluck_wallet.sql not found");
    }

    private static String tableDefinition(String sql, String tableName) {
        Matcher matcher = Pattern.compile("(?is)\\bcreate\\s+table\\s+if\\s+not\\s+exists\\s+"
            + Pattern.quote(tableName) + "\\s*\\((.*?)\\)\\s*engine\\s*=\\s*innodb\\b").matcher(sql);
        assertThat(matcher.find()).as("table %s exists", tableName).isTrue();
        String definition = normalize(matcher.group());
        assertThat(matcher.find()).as("table %s is defined once", tableName).isFalse();
        return definition;
    }

    private static String normalize(String sql) {
        return sql.toLowerCase().replaceAll("\\s+", " ").trim();
    }
}
