package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.payment.enums.PaymentSettlementPayoutActionType;
import com.gameluck.payment.enums.PaymentSettlementPayoutStatus;
import com.gameluck.payment.mapper.PaymentSettlementPayoutActionLogMapper;
import com.gameluck.payment.mapper.PaymentSettlementPayoutMapper;
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
class PaymentSettlementPayoutPersistenceContractTest {

    @Test
    void exposesExactPayoutEnums() {
        assertThat(names(PaymentSettlementPayoutStatus.values())).containsExactly(
            "DRAFT", "PENDING_APPROVAL", "APPROVED", "REJECTED", "CANCELLED");
        assertThat(names(PaymentSettlementPayoutActionType.values())).containsExactly(
            "CREATE", "EDIT", "SUBMIT", "APPROVE", "REJECT", "CANCEL");
    }

    @Test
    void entitiesExposeApprovedNonSensitivePersistenceShape() {
        assertFields(PaymentSettlementPayout.class, fields(
            "id", Long.class, "tenantId", String.class, "payoutNo", String.class,
            "settlementBatchId", Long.class, "settlementNo", String.class,
            "providerCode", String.class, "currencyCode", String.class,
            "payoutAmount", BigDecimal.class, "settlementEvidenceJson", String.class,
            "payoutPurpose", String.class, "payeeReference", String.class,
            "status", String.class, "makerId", Long.class, "makerName", String.class,
            "submitterId", Long.class, "submitterName", String.class,
            "reviewerId", Long.class, "reviewerName", String.class,
            "decisionReason", String.class, "version", Integer.class,
            "submittedTime", Date.class, "reviewedTime", Date.class,
            "createTime", Date.class, "updateTime", Date.class));
        assertFields(PaymentSettlementPayoutActionLog.class, fields(
            "id", Long.class, "tenantId", String.class, "payoutId", Long.class,
            "actionType", String.class, "beforeStatus", String.class, "afterStatus", String.class,
            "operatorId", Long.class, "operatorName", String.class, "reason", String.class,
            "evidenceSnapshotJson", String.class, "expectedVersion", Integer.class,
            "resultVersion", Integer.class, "createTime", Date.class));
        assertThat(allFieldNames()).noneMatch(name -> name.matches(
            ".*(bank|account|routing|card|credential|secret|token|rawbody|signature).*"));
    }

    @Test
    void mappersExposeTenantScopedReadsAndGuardedWrites() throws Exception {
        assertInsert(PaymentSettlementPayoutMapper.class, PaymentSettlementPayout.class,
            "tenant_id", "#{entity.tenantId}", "settlement_batch_id", "#{entity.settlementBatchId}",
            "payout_amount", "#{entity.payoutAmount}", "settlement_evidence_json", "version");
        assertSelect(PaymentSettlementPayoutMapper.class, "selectByTenantAndId",
            new Class<?>[]{String.class, Long.class}, "tenant_id=#{tenantId}", "id=#{id}");
        assertSelect(PaymentSettlementPayoutMapper.class, "selectByTenantAndBatchId",
            new Class<?>[]{String.class, Long.class}, "tenant_id=#{tenantId}",
            "settlement_batch_id=#{batchId}");
        assertSelect(PaymentSettlementPayoutMapper.class, "selectPageByTenant",
            new Class<?>[]{Page.class, String.class, String.class, String.class, String.class,
                String.class, String.class, Date.class, Date.class},
            "tenant_id=#{tenantId}", "payout_no=#{payoutNo}", "settlement_no=#{settlementNo}",
            "status=#{status}", "provider_code=#{providerCode}", "currency_code=#{currencyCode}",
            "create_time &gt;= #{start}", "create_time &lt; #{end}",
            "order by create_time desc,id desc");
        Method edit = declaredMethod(PaymentSettlementPayoutMapper.class, "editDraftOrRejected");
        assertSql(edit, Update.class,
            "tenant_id=#{tenantId}", "id=#{id}", "version=#{version}",
            "status in ('DRAFT','REJECTED')", "status='DRAFT'", "version=version+1",
            "payout_purpose=#{purpose}", "payee_reference=#{payeeReference}",
            "decision_reason=null");
        String editSql = normalize(String.join(" ", edit.getAnnotation(Update.class).value()));
        assertThat(editSql).doesNotContain("maker_id=", "maker_name=", "reviewer_id=",
            "reviewer_name=", "reviewed_time=");
        assertThat(edit.getParameterTypes()).containsExactly(String.class, Long.class, int.class,
            String.class, String.class, Date.class);
        assertUpdate(PaymentSettlementPayoutMapper.class, "transition",
            new Class<?>[]{String.class, Long.class, int.class, String.class, String.class,
                Long.class, String.class, String.class, Date.class},
            "tenant_id=#{tenantId}", "id=#{id}", "version=#{version}", "status=#{expected}",
            "status=#{next}", "version=version+1", "submitter_id=case",
            "reviewer_id=case", "decision_reason=case", "submitted_time=case", "reviewed_time=case");
        assertInsert(PaymentSettlementPayoutActionLogMapper.class,
            PaymentSettlementPayoutActionLog.class, "tenant_id", "#{entity.tenantId}",
            "payout_id", "#{entity.payoutId}", "action_type", "expected_version", "result_version");
        assertSelect(PaymentSettlementPayoutActionLogMapper.class, "selectByPayout",
            new Class<?>[]{String.class, Long.class}, "tenant_id=#{tenantId}",
            "payout_id=#{payoutId}", "order by create_time,id");
    }

    @Test
    void mappersExposeOnlyExplicitPersistenceOperations() {
        assertMethods(PaymentSettlementPayoutMapper.class, "insert", "selectByTenantAndId",
            "selectByTenantAndBatchId", "selectPageByTenant", "editDraftOrRejected", "transition");
        assertMethods(PaymentSettlementPayoutActionLogMapper.class, "insert", "selectByPayout");
        for (Class<?> mapper : new Class<?>[]{PaymentSettlementPayoutMapper.class,
            PaymentSettlementPayoutActionLogMapper.class}) {
            assertThat(mapper.getInterfaces()).isEmpty();
            assertThat(Arrays.stream(mapper.getMethods()).map(Method::getName))
                .noneMatch(name -> name.matches("updateById|update|deleteById|delete|deleteBatchIds"));
        }
    }

    @Test
    void walletSqlDefinesIdempotentTenantScopedPayoutTablesAndMenu() throws Exception {
        String sql = Files.readString(findBackendFile("script/sql/gameluck_wallet.sql"));
        String normalized = normalize(sql);
        String payout = tableDefinition(sql, "gl_payment_settlement_payout");
        assertThat(payout).contains(
            "id bigint not null", "tenant_id varchar(20) not null default '000000'",
            "payout_no varchar(64) not null", "settlement_batch_id bigint not null",
            "settlement_no varchar(64) not null", "provider_code varchar(64) not null",
            "currency_code varchar(32) not null", "payout_amount decimal(20,6) not null",
            "settlement_evidence_json longtext", "payout_purpose varchar(500) not null",
            "payee_reference varchar(128) not null", "status varchar(32) not null",
            "maker_id bigint not null", "version int not null default 0",
            "unique key uk_gl_payment_settlement_payout_01 (tenant_id, payout_no)",
            "unique key uk_gl_payment_settlement_payout_02 (tenant_id, settlement_batch_id)",
            "key idx_gl_payment_settlement_payout_01 (tenant_id, status, create_time, id)",
            "key idx_gl_payment_settlement_payout_02 (tenant_id, settlement_no, id)");
        String logs = tableDefinition(sql, "gl_payment_settlement_payout_action_log");
        assertThat(logs).contains(
            "tenant_id varchar(20) not null default '000000'", "payout_id bigint not null",
            "action_type varchar(32) not null", "evidence_snapshot_json longtext",
            "expected_version int", "result_version int not null",
            "key idx_gl_payment_settlement_payout_action_log_01 (tenant_id, payout_id, create_time, id)");
        for (String table : new String[]{"gl_payment_settlement_payout",
            "gl_payment_settlement_payout_action_log"}) {
            assertThat(normalized).contains("create table if not exists " + table + " (");
            assertThat(normalized).doesNotContain("drop table " + table, "truncate table " + table);
        }
        assertThat(payout + logs).doesNotContain("foreign key", "bank_", "account_", "routing_",
            "card_", "credential", "secret", "token", "raw_body", "signature");

        assertThat(normalized).contains(
            "delete from sys_menu where menu_id in (20351,20352,20353,20354,20355,20356,2035)",
            "(2035,'结算付款审批',1900,9,'payment-settlement-payout','payment/payment-settlement-payout/index'",
            "'payment:settlementpayout:list'",
            "(20351,'结算付款列表',2035,1,'#','','',1,0,'f','0','0','payment:settlementpayout:list'",
            "(20352,'结算付款查询',2035,2,'#','','',1,0,'f','0','0','payment:settlementpayout:query'",
            "(20353,'结算付款创建',2035,3,'#','','',1,0,'f','0','0','payment:settlementpayout:create'",
            "(20354,'结算付款提交',2035,4,'#','','',1,0,'f','0','0','payment:settlementpayout:submit'",
            "(20355,'结算付款审批',2035,5,'#','','',1,0,'f','0','0','payment:settlementpayout:approve'",
            "(20356,'结算付款取消',2035,6,'#','','',1,0,'f','0','0','payment:settlementpayout:cancel'");
        for (String id : new String[]{"2035", "20351", "20352", "20353", "20354", "20355", "20356"}) {
            assertThat(occurrences(sql, "(?m)^\\(" + id + ","))
                .as("menu %s is seeded once", id).isEqualTo(1);
        }
        assertDictionaryValues(normalized);
    }

    @Test
    void platformSqlDefinesIdempotentEnglishPayoutDictionaries() throws Exception {
        String sql = normalize(Files.readString(findBackendFile("script/sql/gameluck_platform_dict.sql")));
        assertThat(sql).contains(
            "payment settlement payout status", "payment settlement payout action type",
            "where not exists", "gl_payment_settlement_payout_status",
            "gl_payment_settlement_payout_action_type");
        assertDictionaryValues(sql);
        assertThat(sql).contains("'draft'", "'pending approval'", "'approved'", "'rejected'",
            "'cancelled'", "'create'", "'edit'", "'submit'", "'approve'", "'reject'", "'cancel'");
    }

    @Test
    void backendBundlesExposeStablePayoutErrors() throws Exception {
        String[] keys = {
            "payment.settlementPayout.input.invalid",
            "payment.settlementPayout.instruction.notFound",
            "payment.settlementPayout.settlement.notFound",
            "payment.settlementPayout.settlement.notClosed",
            "payment.settlementPayout.amount.notPositive",
            "payment.settlementPayout.duplicate",
            "payment.settlementPayout.state.invalid",
            "payment.settlementPayout.version.conflict",
            "payment.settlementPayout.operator.required",
            "payment.settlementPayout.selfApproval",
            "payment.settlementPayout.purpose.invalid",
            "payment.settlementPayout.payeeReference.invalid",
            "payment.settlementPayout.reason.invalid"
        };
        for (String bundle : new String[]{"messages.properties", "messages_en_US.properties",
            "messages_zh_CN.properties"}) {
            String contents = Files.readString(findBackendFile(
                "gameluck-admin/src/main/resources/i18n/" + bundle));
            for (String key : keys) {
                assertThat(contents).as("%s contains %s", bundle, key)
                    .containsPattern("(?m)^" + Pattern.quote(key) + "=.+$");
            }
        }
    }

    private static void assertDictionaryValues(String sql) {
        assertThat(sql).contains("'draft'", "'pending_approval'", "'approved'", "'rejected'",
            "'cancelled'", "'create'", "'edit'", "'submit'", "'approve'", "'reject'", "'cancel'");
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
        return Arrays.stream(new Class<?>[]{PaymentSettlementPayout.class,
            PaymentSettlementPayoutActionLog.class}).flatMap(type -> Arrays.stream(type.getDeclaredFields()))
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
            Arrays.stream(fragments).map(PaymentSettlementPayoutPersistenceContractTest::normalize)
                .toArray(String[]::new));
    }

    private static void assertMethods(Class<?> mapper, String... expected) {
        assertThat(Arrays.stream(mapper.getDeclaredMethods()).map(Method::getName))
            .containsExactlyInAnyOrder(expected);
    }

    private static Method declaredMethod(Class<?> mapper, String name) {
        return Arrays.stream(mapper.getDeclaredMethods()).filter(method -> method.getName().equals(name))
            .findFirst().orElseThrow(() -> new AssertionError("Missing mapper method: " + name));
    }

    private static String tableDefinition(String sql, String table) {
        Matcher matcher = Pattern.compile("(?is)CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS\\s+" + table
            + "\\s*\\((.*?)\\)\\s*ENGINE=").matcher(sql);
        assertThat(matcher.find()).as("table %s exists", table).isTrue();
        return normalize(matcher.group(1));
    }

    private static long occurrences(String value, String regex) {
        return Pattern.compile(regex).matcher(value).results().count();
    }

    private static String normalize(String value) {
        return value.replace('`', ' ').replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private static Path findBackendFile(String relativePath) {
        Path current = Path.of("").toAbsolutePath();
        for (int i = 0; i < 8 && current != null; i++, current = current.getParent()) {
            Path direct = current.resolve(relativePath);
            if (Files.exists(direct)) return direct;
            Path nested = current.resolve("backend").resolve(relativePath);
            if (Files.exists(nested)) return nested;
        }
        throw new IllegalStateException("Cannot locate backend file: " + relativePath);
    }
}
