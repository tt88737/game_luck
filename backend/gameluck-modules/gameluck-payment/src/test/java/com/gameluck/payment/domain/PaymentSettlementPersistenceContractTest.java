package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.payment.enums.PaymentSettlementActionType;
import com.gameluck.payment.enums.PaymentSettlementBatchStatus;
import com.gameluck.payment.mapper.PaymentSettlementActionLogMapper;
import com.gameluck.payment.mapper.PaymentSettlementBatchMapper;
import com.gameluck.payment.mapper.PaymentSettlementItemMapper;
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
class PaymentSettlementPersistenceContractTest {

    @Test
    void exposesExactSettlementEnums() {
        assertThat(names(PaymentSettlementBatchStatus.values())).containsExactly(
            "CREATED", "CALCULATING", "CALCULATED", "CLOSED", "FAILED");
        assertThat(names(PaymentSettlementActionType.values())).containsExactly(
            "CREATE", "CALCULATE", "CALCULATION_FAILED", "CLOSE_REJECTED", "CLOSE");
    }

    @Test
    void entitiesExposeApprovedPersistenceShape() {
        assertFields(PaymentSettlementBatch.class, fields(
            "id", Long.class, "tenantId", String.class, "settlementNo", String.class,
            "providerCode", String.class, "currencyCode", String.class,
            "periodStart", Date.class, "periodEnd", Date.class, "status", String.class,
            "paymentFeeRate", BigDecimal.class, "paymentFixedFee", BigDecimal.class,
            "chargebackFixedFee", BigDecimal.class, "eventCount", Integer.class,
            "paymentCount", Integer.class, "refundCount", Integer.class,
            "chargebackCount", Integer.class, "grossPayment", BigDecimal.class,
            "refundAmount", BigDecimal.class, "chargebackAmount", BigDecimal.class,
            "totalFee", BigDecimal.class, "netSettlement", BigDecimal.class,
            "reconciliationCoverageCount", Integer.class, "openIssueCount", Integer.class,
            "evidenceSnapshotJson", String.class, "failureReason", String.class,
            "creatorId", Long.class, "creatorName", String.class,
            "calculatorId", Long.class, "calculatorName", String.class,
            "closerId", Long.class, "closerName", String.class, "closeRemark", String.class,
            "calculatedTime", Date.class, "closedTime", Date.class, "version", Integer.class,
            "createTime", Date.class, "updateTime", Date.class));
        assertFields(PaymentSettlementItem.class, fields(
            "id", Long.class, "tenantId", String.class, "batchId", Long.class,
            "webhookEventId", Long.class, "providerEventId", String.class,
            "paymentSessionId", Long.class, "sessionNo", String.class,
            "providerSessionNo", String.class, "purchaseOrderId", Long.class,
            "purchaseOrderNo", String.class, "eventType", String.class,
            "receivedTime", Date.class, "currencyCode", String.class, "sourceAmount", BigDecimal.class,
            "grossPayment", BigDecimal.class, "refundAmount", BigDecimal.class,
            "chargebackAmount", BigDecimal.class, "feeAmount", BigDecimal.class,
            "netContribution", BigDecimal.class, "sourceSnapshotJson", String.class,
            "createTime", Date.class));
        assertFields(PaymentSettlementActionLog.class, fields(
            "id", Long.class, "tenantId", String.class, "batchId", Long.class,
            "actionType", String.class, "beforeStatus", String.class, "afterStatus", String.class,
            "operatorId", Long.class, "operatorName", String.class, "remark", String.class,
            "evidenceSnapshotJson", String.class, "createTime", Date.class));
        assertThat(allFieldNames()).noneMatch(name -> name.matches(".*(rawbody|signature|secret|credential|card|instrument).*"));
    }

    @Test
    void mappersExposeTenantScopedReadsAndGuardedWrites() throws Exception {
        assertInsert(PaymentSettlementBatchMapper.class, PaymentSettlementBatch.class,
            "tenant_id", "#{entity.tenantId}", "settlement_no", "#{entity.settlementNo}",
            "payment_fee_rate", "evidence_snapshot_json", "version");
        assertSelect(PaymentSettlementBatchMapper.class, "selectByTenantAndId",
            new Class<?>[]{String.class, Long.class}, "tenant_id=#{tenantId}", "id=#{id}");
        assertSelect(PaymentSettlementBatchMapper.class, "selectPageByTenant",
            new Class<?>[]{Page.class, String.class, String.class, String.class, String.class},
            "tenant_id=#{tenantId}", "order by create_time desc,id desc");
        assertSelect(PaymentSettlementBatchMapper.class, "countOverlapping",
            new Class<?>[]{String.class, String.class, String.class, Date.class, Date.class, Long.class},
            "tenant_id=#{tenantId}", "provider_code=#{providerCode}", "currency_code=#{currencyCode}",
            "period_start &lt; #{periodEnd}", "period_end &gt; #{periodStart}", "status &lt;&gt; 'FAILED'");
        assertUpdate(PaymentSettlementBatchMapper.class, "transitionStatus",
            new Class<?>[]{String.class, Long.class, String.class, String.class, Date.class},
            "tenant_id=#{tenantId}", "id=#{id}", "status=#{expected}",
            "status=#{next}", "version=version+1");
        assertSelect(PaymentSettlementItemMapper.class, "selectPageByBatch",
            new Class<?>[]{Page.class, String.class, Long.class, String.class},
            "tenant_id=#{tenantId}", "batch_id=#{batchId}", "order by received_time,id");
        assertInsert(PaymentSettlementItemMapper.class, PaymentSettlementItem.class,
            "tenant_id", "#{entity.tenantId}", "webhook_event_id", "source_snapshot_json",
            "#{entity.sourceSnapshotJson}");
        assertSelect(PaymentSettlementActionLogMapper.class, "selectByBatch",
            new Class<?>[]{String.class, Long.class}, "tenant_id=#{tenantId}",
            "batch_id=#{batchId}", "order by create_time,id");
        assertInsert(PaymentSettlementActionLogMapper.class, PaymentSettlementActionLog.class,
            "tenant_id", "#{entity.tenantId}", "action_type", "evidence_snapshot_json");
    }

    @Test
    void mappersExposeOnlyExplicitPersistenceOperations() {
        assertMethods(PaymentSettlementBatchMapper.class, "insert", "selectByTenantAndId",
            "selectPageByTenant", "countOverlapping", "transitionStatus", "completeCalculation",
            "markFailed", "closeCalculated");
        assertMethods(PaymentSettlementItemMapper.class, "insert", "insertBatch", "selectPageByBatch");
        assertMethods(PaymentSettlementActionLogMapper.class, "insert", "selectByBatch");
        for (Class<?> mapper : new Class<?>[]{PaymentSettlementBatchMapper.class,
            PaymentSettlementItemMapper.class, PaymentSettlementActionLogMapper.class}) {
            assertThat(mapper.getInterfaces()).isEmpty();
            assertThat(Arrays.stream(mapper.getMethods()).map(Method::getName))
                .noneMatch(name -> name.matches("updateById|update|deleteById|delete|deleteBatchIds"));
        }
    }

    @Test
    void walletSqlDefinesIdempotentTenantScopedSettlementTables() throws Exception {
        String sql = Files.readString(findWalletSql());
        String normalized = normalize(sql);
        String batches = tableDefinition(sql, "gl_payment_settlement_batch");
        assertThat(batches).contains(
            "id bigint not null", "tenant_id varchar(20) not null default '000000'",
            "settlement_no varchar(64) not null", "provider_code varchar(64) not null",
            "currency_code varchar(32) not null", "period_start datetime not null", "period_end datetime not null",
            "payment_fee_rate decimal(12,8) not null", "payment_fixed_fee decimal(20,6) not null",
            "chargeback_fixed_fee decimal(20,6) not null", "gross_payment decimal(20,6) not null default 0",
            "refund_amount decimal(20,6) not null default 0", "chargeback_amount decimal(20,6) not null default 0",
            "total_fee decimal(20,6) not null default 0", "net_settlement decimal(20,6) not null default 0",
            "evidence_snapshot_json longtext", "version int not null default 0",
            "unique key uk_gl_payment_settlement_batch_01 (tenant_id, settlement_no)",
            "unique key uk_gl_payment_settlement_batch_02 (tenant_id, provider_code, currency_code, period_start, period_end)",
            "key idx_gl_payment_settlement_batch_01 (tenant_id, status, period_start, id)",
            "key idx_gl_payment_settlement_batch_02 (tenant_id, provider_code, currency_code, period_start, period_end)");
        String items = tableDefinition(sql, "gl_payment_settlement_item");
        assertThat(items).contains(
            "tenant_id varchar(20) not null default '000000'", "batch_id bigint not null",
            "webhook_event_id bigint not null", "source_amount decimal(20,6) not null",
            "fee_amount decimal(20,6) not null", "net_contribution decimal(20,6) not null",
            "source_snapshot_json longtext not null",
            "unique key uk_gl_payment_settlement_item_01 (tenant_id, webhook_event_id)",
            "key idx_gl_payment_settlement_item_01 (tenant_id, batch_id, received_time, id)");
        String logs = tableDefinition(sql, "gl_payment_settlement_action_log");
        assertThat(logs).contains(
            "tenant_id varchar(20) not null default '000000'", "batch_id bigint not null",
            "action_type varchar(32) not null", "evidence_snapshot_json longtext",
            "key idx_gl_payment_settlement_action_log_01 (tenant_id, batch_id, create_time, id)");
        for (String table : new String[]{"gl_payment_settlement_batch", "gl_payment_settlement_item",
            "gl_payment_settlement_action_log"}) {
            assertThat(normalized).contains("create table if not exists " + table + " (");
            assertThat(normalized).doesNotContain("drop table " + table, "truncate table " + table);
        }
        assertThat(batches + items + logs).doesNotContain(
            "foreign key", "raw_body", "signature", "secret", "credential", "card_number", "payment_instrument");
    }

    private static String[] names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toArray(String[]::new);
    }

    private static Map<String, Class<?>> fields(Object... namesAndTypes) {
        Map<String, Class<?>> result = new LinkedHashMap<>();
        for (int i = 0; i < namesAndTypes.length; i += 2) {
            result.put((String) namesAndTypes[i], (Class<?>) namesAndTypes[i + 1]);
        }
        return result;
    }

    private static void assertFields(Class<?> type, Map<String, Class<?>> expected) {
        Map<String, Class<?>> actual = new LinkedHashMap<>();
        Arrays.stream(type.getDeclaredFields()).filter(field -> !field.isSynthetic())
            .forEach(field -> actual.put(field.getName(), field.getType()));
        assertThat(actual).containsExactlyEntriesOf(expected);
    }

    private static java.util.stream.Stream<String> allFieldNames() {
        return Arrays.stream(new Class<?>[]{PaymentSettlementBatch.class, PaymentSettlementItem.class,
            PaymentSettlementActionLog.class}).flatMap(type -> Arrays.stream(type.getDeclaredFields()))
            .map(Field::getName).map(String::toLowerCase);
    }

    private static void assertSelect(Class<?> mapper, String name, Class<?>[] types, String... fragments)
            throws Exception {
        assertSql(mapper.getMethod(name, types), Select.class, fragments);
    }

    private static void assertUpdate(Class<?> mapper, String name, Class<?>[] types, String... fragments)
            throws Exception {
        assertSql(mapper.getMethod(name, types), Update.class, fragments);
    }

    private static void assertInsert(Class<?> mapper, Class<?> entityType, String... fragments) throws Exception {
        assertSql(mapper.getMethod("insert", entityType), Insert.class, fragments);
    }

    private static void assertSql(Method method, Class<? extends java.lang.annotation.Annotation> annotation,
                                  String... fragments) {
        String[] value;
        if (annotation == Select.class) value = method.getAnnotation(Select.class).value();
        else if (annotation == Update.class) value = method.getAnnotation(Update.class).value();
        else value = method.getAnnotation(Insert.class).value();
        assertThat(normalize(String.join(" ", value))).contains(
            Arrays.stream(fragments).map(PaymentSettlementPersistenceContractTest::normalize).toArray(String[]::new));
    }

    private static void assertMethods(Class<?> mapper, String... expected) {
        assertThat(Arrays.stream(mapper.getDeclaredMethods()).map(Method::getName))
            .containsExactlyInAnyOrder(expected);
    }

    private static String tableDefinition(String sql, String table) {
        Matcher matcher = Pattern.compile("(?is)CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS\\s+" + table
            + "\\s*\\((.*?)\\)\\s*ENGINE=").matcher(sql);
        assertThat(matcher.find()).as("table %s exists", table).isTrue();
        return normalize(matcher.group(1));
    }

    private static String normalize(String value) {
        return value.replace('`', ' ').replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private static Path findWalletSql() {
        Path current = Path.of("").toAbsolutePath();
        for (int i = 0; i < 8 && current != null; i++, current = current.getParent()) {
            Path direct = current.resolve("script/sql/gameluck_wallet.sql");
            if (Files.exists(direct)) return direct;
            Path nested = current.resolve("backend/script/sql/gameluck_wallet.sql");
            if (Files.exists(nested)) return nested;
        }
        throw new IllegalStateException("Cannot locate gameluck_wallet.sql");
    }
}
