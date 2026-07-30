package com.gameluck.payment.domain;

import com.gameluck.payment.enums.PaymentProviderEventType;
import com.gameluck.payment.enums.PaymentSessionStatus;
import com.gameluck.payment.enums.PaymentWebhookEventStatus;
import com.gameluck.payment.mapper.PaymentSessionMapper;
import com.gameluck.payment.mapper.PaymentWebhookEventMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.mapper.PurchaseOrderGrantSnapshotMapper;
import com.gameluck.payment.mapper.SimulatedPaymentDispatchMapper;
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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("local")
class PaymentProviderPersistenceContractTest {

    @Test
    void exposesExactProviderPersistenceEnums() {
        assertThat(names(PaymentSessionStatus.values())).containsExactly(
            "CREATED", "PENDING", "SUCCEEDED", "FAILED", "CANCELLED", "EXPIRED");
        assertThat(names(PaymentWebhookEventStatus.values())).containsExactly(
            "RECEIVED", "PROCESSED", "FAILED", "IGNORED");
        assertThat(names(PaymentProviderEventType.values())).containsExactly(
            "PAYMENT_SUCCEEDED", "PAYMENT_FAILED", "PAYMENT_CANCELLED",
            "REFUND_SUCCEEDED", "CHARGEBACK_CREATED");
    }

    @Test
    void paymentSessionContainsCompletePersistenceShape() {
        assertFields(PaymentSession.class, fields(
            "id", Long.class, "tenantId", String.class, "sessionNo", String.class,
            "purchaseOrderId", Long.class, "purchaseOrderNo", String.class,
            "memberId", Long.class, "providerCode", String.class,
            "providerSessionNo", String.class, "payCurrencyCode", String.class,
            "payAmount", BigDecimal.class, "checkoutUrl", String.class,
            "status", String.class, "requestKey", String.class,
            "expireTime", Date.class, "completedTime", Date.class,
            "version", Integer.class, "createTime", Date.class, "updateTime", Date.class));
    }

    @Test
    void webhookEventContainsCompletePersistenceShapeWithoutProviderSecret() {
        assertFields(PaymentWebhookEvent.class, fields(
            "id", Long.class, "tenantId", String.class, "providerCode", String.class,
            "providerEventId", String.class, "eventType", String.class,
            "providerSessionNo", String.class, "sessionNo", String.class,
            "purchaseOrderNo", String.class, "rawBody", String.class,
            "signatureDigest", String.class, "receivedTime", Date.class,
            "status", String.class, "failureReason", String.class,
            "processingCount", Integer.class, "lastProcessingTime", Date.class,
            "createTime", Date.class, "updateTime", Date.class));
        assertThat(Arrays.stream(PaymentWebhookEvent.class.getDeclaredFields())
            .map(Field::getName)).noneMatch(name -> name.toLowerCase().contains("secret"));
    }

    @Test
    void simulatedDispatchMarkerContainsOnlyImmutableRoutingFacts() {
        assertFields(SimulatedPaymentDispatch.class, fields(
            "id", Long.class, "tenantId", String.class, "providerSessionNo", String.class,
            "providerEventId", String.class, "action", String.class,
            "occurredTime", Date.class, "createTime", Date.class));
        assertThat(Arrays.stream(SimulatedPaymentDispatch.class.getDeclaredFields()).map(Field::getName))
            .noneMatch(name -> name.toLowerCase().matches(".*(secret|signature|rawbody|payload).*"));
    }

    @Test
    void mappersExposeTenantScopedQueriesAndLockingContracts() throws Exception {
        assertSelect(PaymentSessionMapper.class, "selectByRequestKey",
            new Class<?>[]{String.class, String.class}, "tenant_id = #{tenantId}", "request_key = #{requestKey}");
        assertSelect(PaymentSessionMapper.class, "selectByRequestKeyForUpdate",
            new Class<?>[]{String.class, String.class}, "tenant_id = #{tenantId}", "request_key = #{requestKey}", "for update");
        assertSelect(PurchaseOrderMapper.class, "selectByIdempotencyKeyForUpdate",
            new Class<?>[]{String.class, String.class}, "tenant_id = #{tenantId}", "idempotency_key = #{idempotencyKey}", "for update");
        assertSelect(PurchaseOrderGrantSnapshotMapper.class, "selectByPurchaseOrderNoForUpdate",
            new Class<?>[]{String.class, String.class}, "tenant_id = #{tenantId}",
            "purchase_order_no = #{purchaseOrderNo}", "order by", "for update");
        assertSelect(PaymentSessionMapper.class, "selectBySessionNo",
            new Class<?>[]{String.class, String.class}, "tenant_id = #{tenantId}", "session_no = #{sessionNo}");
        assertSelect(PaymentSessionMapper.class, "selectBySessionNoForUpdate",
            new Class<?>[]{String.class, String.class}, "tenant_id = #{tenantId}", "session_no = #{sessionNo}", "for update");
        assertSelect(PaymentSessionMapper.class, "selectActiveByOrderNoForUpdate",
            new Class<?>[]{String.class, String.class, Date.class}, "tenant_id = #{tenantId}",
            "purchase_order_no = #{purchaseOrderNo}", "status in ('created', 'pending')",
            "expire_time > #{now}", "for update");
        assertSelect(PaymentWebhookEventMapper.class, "selectByProviderEventId",
            new Class<?>[]{String.class, String.class, String.class}, "tenant_id = #{tenantId}",
            "provider_code = #{providerCode}", "provider_event_id = #{providerEventId}");
        assertSelect(PaymentWebhookEventMapper.class, "selectByIdForUpdate",
            new Class<?>[]{String.class, Long.class}, "tenant_id = #{tenantId}", "id = #{id}", "for update");
        assertSelect(SimulatedPaymentDispatchMapper.class, "selectLatestDelivered",
            new Class<?>[]{String.class, String.class}, "e.tenant_id = d.tenant_id",
            "e.provider_session_no = d.provider_session_no", "e.provider_event_id = d.provider_event_id",
            "e.status in ('processed', 'ignored')", "d.tenant_id = #{tenantId}",
            "d.provider_session_no = #{providerSessionNo}");
        assertSelect(SimulatedPaymentDispatchMapper.class, "selectLatestReplayable",
            new Class<?>[]{String.class, String.class}, "e.tenant_id = d.tenant_id",
            "e.provider_session_no = d.provider_session_no", "e.provider_event_id = d.provider_event_id",
            "e.status in ('failed', 'processed', 'ignored')", "d.tenant_id = #{tenantId}");
    }

    @Test
    void webhookFailureUpdateRetriesOnlyNonTerminalEventsAndCountsEachAttempt() throws Exception {
        Method method = PaymentWebhookEventMapper.class.getMethod("recordFailure",
            String.class, Long.class, String.class, Date.class);
        String query = String.join(" ", method.getAnnotation(Update.class).value()).toLowerCase();

        assertThat(query).contains(
            "tenant_id = #{tenantid}",
            "id = #{id}",
            "status in ('received', 'failed')",
            "processing_count = processing_count + 1",
            "last_processing_time = #{now}");
        assertThat(query).doesNotContain("processed", "ignored");
    }

    @Test
    void walletSqlDefinesProviderSessionAndWebhookTables() throws Exception {
        String sql = Files.readString(findWalletSql());
        String normalizedSql = sql.toLowerCase();
        assertThat(tableDefinition(sql, "gl_purchase_order")).contains(
            "offer_name_snapshot varchar(128) default null");
        assertThat(sql.toLowerCase()).contains(
            "column_name = 'offer_name_snapshot'",
            "alter table gl_purchase_order add column offer_name_snapshot");
        String grantSnapshots = tableDefinition(sql, "gl_purchase_order_grant_snapshot");
        assertThat(grantSnapshots).contains(
            "wagering_multiplier decimal(10,4)", "wagering_expire_days int");
        assertThat(sql.toLowerCase()).contains(
            "column_name = 'wagering_multiplier'", "column_name = 'wagering_expire_days'");
        int snapshotCreate = normalizedSql.indexOf("create table if not exists gl_purchase_order_grant_snapshot (");
        assertThat(snapshotCreate).isLessThan(normalizedSql.indexOf("column_name = 'wagering_multiplier'"));
        assertThat(snapshotCreate).isLessThan(normalizedSql.indexOf("column_name = 'wagering_expire_days'"));
        assertThat(normalizedSql.indexOf("create table if not exists gl_purchase_order ("))
            .isLessThan(normalizedSql.indexOf("column_name = 'offer_name_snapshot'"));
        String sessions = tableDefinition(sql, "gl_payment_session");
        assertThat(sessions).contains(
            "id bigint not null", "tenant_id varchar(20) not null default '000000'",
            "session_no varchar(64) not null", "purchase_order_id bigint not null",
            "purchase_order_no varchar(64) not null", "member_id bigint not null",
            "provider_code varchar(64) not null", "provider_session_no varchar(128) default null",
            "pay_currency_code varchar(32) not null", "pay_amount decimal(20,6) not null",
            "checkout_url varchar(1000) default null", "status varchar(32) not null",
            "request_key varchar(128) not null", "expire_time datetime not null",
            "completed_time datetime default null", "version int not null default 0",
            "create_time datetime not null default current_timestamp",
            "update_time datetime default null on update current_timestamp",
            "primary key (id)",
            "unique key uk_gl_payment_session_01 (tenant_id, session_no)",
            "unique key uk_gl_payment_session_02 (tenant_id, request_key)",
            "unique key uk_gl_payment_session_03 (tenant_id, provider_code, provider_session_no)",
            "key idx_gl_payment_session_01 (tenant_id, purchase_order_no)",
            "key idx_gl_payment_session_02 (tenant_id, member_id, status, create_time)",
            "key idx_gl_payment_session_03 (tenant_id, status, expire_time)",
            "key idx_gl_payment_session_04 (tenant_id, create_time, id)",
            "key idx_gl_payment_session_05 (tenant_id, provider_session_no)");
        assertFalse(sessions.contains("secret"), "payment session table must not persist provider secrets");

        String events = tableDefinition(sql, "gl_payment_webhook_event");
        assertThat(events).contains(
            "id bigint not null", "tenant_id varchar(20) not null default '000000'",
            "provider_code varchar(64) not null", "provider_event_id varchar(128) not null",
            "event_type varchar(32) not null", "provider_session_no varchar(128) default null",
            "session_no varchar(64) default null", "purchase_order_no varchar(64) default null",
            "raw_body longtext not null", "signature_digest varchar(128) default null",
            "received_time datetime not null", "status varchar(32) not null",
            "failure_reason varchar(500) default null", "processing_count int not null default 0",
            "last_processing_time datetime default null",
            "create_time datetime not null default current_timestamp",
            "update_time datetime default null on update current_timestamp",
            "primary key (id)",
            "unique key uk_gl_payment_webhook_event_01 (tenant_id, provider_code, provider_event_id)",
            "key idx_gl_payment_webhook_event_01 (tenant_id, session_no)",
            "key idx_gl_payment_webhook_event_02 (tenant_id, purchase_order_no)",
            "key idx_gl_payment_webhook_event_03 (tenant_id, status, received_time)",
            "key idx_gl_payment_webhook_event_04 (tenant_id, provider_code, provider_session_no)",
            "key idx_gl_payment_webhook_event_05 (tenant_id, received_time, id)",
            "key idx_gl_payment_webhook_event_06 (tenant_id, provider_event_id)",
            "key idx_gl_payment_webhook_event_07 (tenant_id, provider_session_no)");
        assertFalse(events.contains("secret"), "webhook event table must not persist provider secrets");
        for (String index : List.of("idx_gl_payment_session_04", "idx_gl_payment_session_05",
            "idx_gl_payment_webhook_event_05", "idx_gl_payment_webhook_event_06",
            "idx_gl_payment_webhook_event_07")) {
            assertThat(normalizedSql).contains("information_schema.statistics", "index_name = '" + index + "'");
            assertThat(count(normalizedSql, "index_name = '" + index + "'")).isEqualTo(1);
        }

        String dispatches = tableDefinition(sql, "gl_simulated_payment_dispatch");
        assertThat(dispatches).contains(
            "tenant_id varchar(20) not null default '000000'",
            "provider_session_no varchar(128) not null",
            "provider_event_id varchar(128) not null",
            "action varchar(32) not null", "occurred_time datetime not null",
            "unique key uk_gl_simulated_payment_dispatch_01 (tenant_id, provider_event_id)",
            "key idx_gl_simulated_payment_dispatch_01 (tenant_id, provider_session_no, create_time)");
        assertFalse(dispatches.contains("secret"));
        assertFalse(dispatches.contains("raw_body"));
        assertFalse(dispatches.contains("signature"));
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }

    private static Map<String, Class<?>> fields(Object... namesAndTypes) {
        Map<String, Class<?>> result = new LinkedHashMap<>();
        for (int index = 0; index < namesAndTypes.length; index += 2) {
            result.put((String) namesAndTypes[index], (Class<?>) namesAndTypes[index + 1]);
        }
        return result;
    }

    private static void assertFields(Class<?> type, Map<String, Class<?>> expected) {
        Map<String, Class<?>> actual = new LinkedHashMap<>();
        Arrays.stream(type.getDeclaredFields()).filter(field -> !field.isSynthetic())
            .forEach(field -> actual.put(field.getName(), field.getType()));
        assertThat(actual).containsExactlyEntriesOf(expected);
    }

    private static void assertSelect(Class<?> mapper, String methodName, Class<?>[] parameterTypes,
                                     String... fragments) throws Exception {
        Method method = mapper.getMethod(methodName, parameterTypes);
        String query = String.join(" ", method.getAnnotation(Select.class).value()).toLowerCase();
        assertThat(query).contains(Arrays.stream(fragments).map(String::toLowerCase).toArray(String[]::new));
    }

    private static Path findWalletSql() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("script/sql/gameluck_wallet.sql");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new AssertionError("script/sql/gameluck_wallet.sql not found from working directory");
    }

    private static String tableDefinition(String sql, String tableName) {
        Pattern createTable = Pattern.compile(
            "(?is)\\bcreate\\s+table\\s+if\\s+not\\s+exists\\s+"
                + Pattern.quote(tableName)
                + "\\s*\\((.*?)\\)\\s*engine\\s*=\\s*innodb\\b");
        Matcher matcher = createTable.matcher(sql);
        assertThat(matcher.find()).as("table %s exists", tableName).isTrue();
        String definition = matcher.group().toLowerCase().replaceAll("\\s+", " ").trim();
        assertThat(matcher.find()).as("table %s is defined once", tableName).isFalse();
        return definition;
    }

    private static int count(String text, String fragment) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(fragment, at)) >= 0; at += fragment.length()) count++;
        return count;
    }
}
