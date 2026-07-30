package com.gameluck.payment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.domain.PaymentReconciliationLine;
import com.gameluck.payment.mapper.*;
import com.gameluck.payment.service.reconciliation.*;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Tag("local")
@SpringJUnitConfig
@ContextConfiguration(classes = PaymentReconciliationExecutionTransactionIntegrationTest.Config.class)
@TestPropertySource(properties = "tenant.enable=true")
class PaymentReconciliationExecutionTransactionIntegrationTest {
    @Autowired PaymentReconciliationExecutionService execution;
    @Autowired PaymentReconciliationFailureRecorder failureRecorder;
    @Autowired TestPlatformDataSource platform;
    @Autowired JdbcTemplate jdbc;
    @Autowired PaymentWebhookEventMapper webhookMapper;

    @BeforeEach
    void schema() {
        jdbc.execute("drop table if exists gl_payment_reconciliation_action_log");
        jdbc.execute("drop table if exists gl_payment_reconciliation_issue");
        jdbc.execute("drop table if exists gl_payment_reconciliation_line");
        jdbc.execute("drop table if exists gl_payment_reconciliation_batch");
        jdbc.execute("drop table if exists gl_payment_webhook_event");
        jdbc.execute("drop table if exists gl_payment_session");
        jdbc.execute("drop table if exists gl_purchase_reversal");
        jdbc.execute("create table gl_payment_reconciliation_batch(id bigint primary key,tenant_id varchar(64),provider_code varchar(32),statement_date date,original_file_name varchar(255),file_digest varchar(64),total_count int,valid_count int,invalid_count int,matched_count int,discrepancy_count int,status varchar(32),failure_reason varchar(255),creator_id bigint,creator_name varchar(64),version int,create_time timestamp,update_time timestamp)");
        jdbc.execute("create table gl_payment_reconciliation_line(id bigint primary key,tenant_id varchar(64),batch_id bigint,source_row_number bigint,provider_record_id varchar(128),event_type varchar(64),provider_session_no varchar(128),purchase_order_no varchar(128),currency_code varchar(8),amount decimal(20,6),occurred_time timestamp,status varchar(32),parse_error varchar(255),raw_fields_json clob,create_time timestamp)");
        jdbc.execute("create table gl_payment_reconciliation_issue(id bigint primary key,tenant_id varchar(64),batch_id bigint,line_id bigint,issue_type varchar(64),status varchar(32),payment_session_id bigint,session_no varchar(128),purchase_order_id bigint,purchase_order_no varchar(128),webhook_event_id bigint check(webhook_event_id is null or webhook_event_id <> 999),reversal_id bigint,provider_event_type varchar(64),platform_event_type varchar(64),provider_currency_code varchar(8),platform_currency_code varchar(8),provider_amount decimal(20,6),platform_amount decimal(20,6),provider_status varchar(32),platform_status varchar(32),diagnostic_snapshot_json clob,resolution_type varchar(32),resolution_remark varchar(255),resolved_by bigint,resolved_time timestamp,version int,create_time timestamp,update_time timestamp)");
        jdbc.execute("create table gl_payment_reconciliation_action_log(id bigint primary key,tenant_id varchar(64),batch_id bigint,issue_id bigint,action_type varchar(64),before_status varchar(32),after_status varchar(32),operator_id bigint not null,operator_name varchar(64) not null,remark varchar(255),create_time timestamp)");
        jdbc.execute("create table gl_payment_webhook_event(id bigint primary key,tenant_id varchar(64),provider_code varchar(32),provider_event_id varchar(128),event_type varchar(64),provider_session_no varchar(128),purchase_order_no varchar(128),received_time timestamp,status varchar(32))");
        jdbc.execute("create table gl_payment_session(id bigint primary key,tenant_id varchar(64),provider_code varchar(32),provider_session_no varchar(128),purchase_order_id bigint,pay_currency_code varchar(8),pay_amount decimal(20,6))");
        jdbc.execute("create table gl_purchase_reversal(id bigint primary key,tenant_id varchar(64),purchase_order_no varchar(128),create_time timestamp)");
        platform.mode = Mode.MATCHED;
        platform.prefetchCalls.set(0);
        platform.emittedPageSizes.clear();
    }

    @Test
    void guardedAcquireAllowsExactlyOneConcurrentWinner() throws Exception {
        seedBatch(10, "VALIDATED", 0, 0); seedLine(101, 10);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
        Callable<Boolean> request = () -> {
            ready.countDown();
            try { go.await(); execution.acquire("tenant-a", 10L); return true; }
            catch (ServiceException conflict) { assertEquals(com.gameluck.common.core.utils.MessageUtils.message("payment.reconciliation.execute.stateConflict"), conflict.getMessage()); return false; }
            catch (InterruptedException interrupted) { throw new RuntimeException(interrupted); }
        };
        Future<Boolean> first = pool.submit(request), second = pool.submit(request);
        assertTrue(ready.await(5, TimeUnit.SECONDS)); go.countDown();
        assertEquals(1, List.of(first.get(), second.get()).stream().filter(Boolean::booleanValue).count());
        assertEquals("RECONCILING", text("select status from gl_payment_reconciliation_batch where id=10"));
        pool.shutdownNow();
    }

    @Test
    void phaseBRollsBackAfterPartialWritesAndPhaseCPersistsFailureIndependently() {
        seedBatch(20, "RECONCILING", 0, 1); seedLine(201, 20);
        platform.mode = Mode.FAIL_AFTER_FIRST_ISSUE;
        var lease = new PaymentReconciliationExecutionService.ExecutionLease("tenant-a", 20L, 1);

        assertThrows(RuntimeException.class, () -> execution.reconcile(lease));
        assertEquals(0, count("select count(*) from gl_payment_reconciliation_issue where batch_id=20"));
        assertEquals("VALID", text("select status from gl_payment_reconciliation_line where id=201"));
        assertEquals(0, count("select count(*) from gl_payment_reconciliation_action_log where batch_id=20"));
        assertEquals("RECONCILING", text("select status from gl_payment_reconciliation_batch where id=20"));

        failureRecorder.recordFailure("tenant-a", 20L, "CSV /internal SQL secret");
        assertEquals("FAILED", text("select status from gl_payment_reconciliation_batch where id=20"));
        assertEquals("Reconciliation execution failed", text("select failure_reason from gl_payment_reconciliation_batch where id=20"));
        assertEquals(1, count("select count(*) from gl_payment_reconciliation_action_log where batch_id=20 and action_type='EXECUTION_FAILED'"));
        assertEquals(0, count("select operator_id from gl_payment_reconciliation_action_log where batch_id=20"));
        assertEquals("SYSTEM", text("select operator_name from gl_payment_reconciliation_action_log where batch_id=20"));
        failureRecorder.recordFailure("tenant-a", 20L, "again");
        assertEquals(1, count("select count(*) from gl_payment_reconciliation_action_log where batch_id=20"));
    }

    @Test
    void successfulPhaseBCommitsConclusionsCountsPlatformMissingIssueAndExactLog() {
        seedBatch(30, "RECONCILING", 0, 1); seedLine(301, 30);
        platform.mode = Mode.MATCHED_WITH_PLATFORM_MISSING;
        execution.reconcile(new PaymentReconciliationExecutionService.ExecutionLease("tenant-a", 30L, 1));
        assertEquals("MATCHED", text("select status from gl_payment_reconciliation_line where id=301"));
        assertEquals("COMPLETED", text("select status from gl_payment_reconciliation_batch where id=30"));
        assertEquals(1, count("select matched_count from gl_payment_reconciliation_batch where id=30"));
        assertEquals(1, count("select discrepancy_count from gl_payment_reconciliation_batch where id=30"));
        assertEquals("PROVIDER_RECORD_MISSING", text("select issue_type from gl_payment_reconciliation_issue where batch_id=30"));
        assertEquals(77, count("select webhook_event_id from gl_payment_reconciliation_issue where batch_id=30"));
        assertEquals(7001, count("select payment_session_id from gl_payment_reconciliation_issue where batch_id=30"));
        assertEquals(7002, count("select purchase_order_id from gl_payment_reconciliation_issue where batch_id=30"));
        assertEquals(7003, count("select reversal_id from gl_payment_reconciliation_issue where batch_id=30"));
        String diagnostic = text("select diagnostic_snapshot_json from gl_payment_reconciliation_issue where batch_id=30");
        assertTrue(diagnostic.contains("\"providerAbsent\":true"));
        assertTrue(diagnostic.contains("\"providerEventId\":\"missing-77\""));
        assertTrue(diagnostic.contains("\"providerSessionNo\":\"ps-platform\""));
        assertTrue(diagnostic.contains("\"purchaseOrderNo\":\"order-platform\""));
        assertTrue(diagnostic.contains("\"currency\":\"USD\""));
        assertTrue(diagnostic.contains("\"amount\":12.340000"));
        assertFalse(diagnostic.toLowerCase().contains("raw_body"));
        assertEquals(1, count("select count(*) from gl_payment_reconciliation_action_log where batch_id=30 and action_type='EXECUTE' and before_status='VALIDATED' and after_status='COMPLETED'"));
    }

    @Test
    void lineIssuePersistsAllResolvedPlatformIdentifiers() {
        seedBatch(40, "RECONCILING", 0, 1); seedLine(401, 40);
        platform.mode = Mode.ISSUE_WITH_IDS;
        execution.reconcile(new PaymentReconciliationExecutionService.ExecutionLease("tenant-a", 40L, 1));
        assertEquals("ISSUE", text("select status from gl_payment_reconciliation_line where id=401"));
        assertEquals(8001, count("select payment_session_id from gl_payment_reconciliation_issue where batch_id=40"));
        assertEquals(8002, count("select purchase_order_id from gl_payment_reconciliation_issue where batch_id=40"));
        assertEquals(8003, count("select webhook_event_id from gl_payment_reconciliation_issue where batch_id=40"));
        assertEquals(8004, count("select reversal_id from gl_payment_reconciliation_issue where batch_id=40"));
    }

    @Test
    void fiveHundredAndOneLinesUseTwoPrefetchesAndBatchConclusions() {
        seedBatch(50, "RECONCILING", 0, 1);
        for (int index = 0; index < 501; index++) seedLine(50_001L + index, 50);
        execution.reconcile(new PaymentReconciliationExecutionService.ExecutionLease("tenant-a", 50L, 1));
        assertEquals(2, platform.prefetchCalls.get());
        assertEquals(501, count("select count(*) from gl_payment_reconciliation_line where batch_id=50 and status='MATCHED'"));
        assertEquals(0, count("select count(*) from gl_payment_reconciliation_issue where batch_id=50"));
        assertEquals(501, count("select matched_count from gl_payment_reconciliation_batch where id=50"));
    }

    @Test
    void moreThanOneThousandMissingEventsInsertAsBoundedPages() {
        seedBatch(60, "RECONCILING", 0, 1);
        platform.mode = Mode.MANY_MISSING;
        execution.reconcile(new PaymentReconciliationExecutionService.ExecutionLease("tenant-a", 60L, 1));
        assertEquals(List.of(500, 500, 3), platform.emittedPageSizes);
        assertEquals(1003, count("select count(*) from gl_payment_reconciliation_issue where batch_id=60"));
        assertEquals(1003, count("select discrepancy_count from gl_payment_reconciliation_batch where id=60"));
        assertTrue(platform.emittedPageSizes.stream().allMatch(size -> size <= 500));
    }

    @Test
    void missingProjectionChoosesLatestReversalByTimeBeforeId() {
        seedBatch(70, "RECONCILING", 0, 1);
        jdbc.update("insert into gl_payment_webhook_event values(701,'tenant-a','SIMULATED','event-701','PAYMENT_SUCCEEDED','ps-701','order-701',timestamp '2026-07-28 01:00:00','PROCESSED')");
        jdbc.update("insert into gl_purchase_reversal values(900,'tenant-a','order-701',timestamp '2026-07-27 12:00:00')");
        jdbc.update("insert into gl_purchase_reversal values(800,'tenant-a','order-701',timestamp '2026-07-28 02:00:00')");
        List<ReconciliationPlatformEventProjection> rows = webhookMapper.selectReconciliationStatementEvents(
            "tenant-a", 70L, "SIMULATED", Instant.parse("2026-07-27T00:00:00Z"),
            Instant.parse("2026-07-30T00:00:00Z"), null, null, 500);
        assertEquals(1, rows.size());
        assertEquals(800L, rows.get(0).reversalId());
    }

    private void seedBatch(long id, String status, int invalid, int version) {
        jdbc.update("insert into gl_payment_reconciliation_batch(id,tenant_id,provider_code,statement_date,original_file_name,file_digest,total_count,valid_count,invalid_count,matched_count,discrepancy_count,status,version,create_time) values(?, 'tenant-a','SIMULATED',date '2026-07-28','x.csv',?,1,1,?,0,0,?,?,current_timestamp)", id, "d"+id, invalid, status, version);
    }
    private void seedLine(long id, long batch) {
        jdbc.update("insert into gl_payment_reconciliation_line(id,tenant_id,batch_id,source_row_number,provider_record_id,event_type,provider_session_no,purchase_order_no,currency_code,amount,occurred_time,status,raw_fields_json,create_time) values(?,'tenant-a',?,2,'provider-1','PAYMENT_SUCCEEDED','ps-1','order-1','USD',12.34,current_timestamp,'VALID','[]',current_timestamp)", id, batch);
    }
    private int count(String sql) { return jdbc.queryForObject(sql, Integer.class); }
    private String text(String sql) { return jdbc.queryForObject(sql, String.class); }

    enum Mode { MATCHED, MATCHED_WITH_PLATFORM_MISSING, ISSUE_WITH_IDS, FAIL_AFTER_FIRST_ISSUE, MANY_MISSING }
    static class TestPlatformDataSource implements PaymentReconciliationExecutionService.PlatformDataSource {
        volatile Mode mode = Mode.MATCHED;
        final AtomicInteger prefetchCalls = new AtomicInteger();
        final List<Integer> emittedPageSizes = new java.util.concurrent.CopyOnWriteArrayList<>();
        public ReconciliationPlatformSnapshot snapshot(String tenant, String provider, PaymentReconciliationLine line) {
            boolean mismatch = mode == Mode.FAIL_AFTER_FIRST_ISSUE || mode == Mode.ISSUE_WITH_IDS;
            return new ReconciliationPlatformSnapshot(1,"order-1","USD",mismatch ? new BigDecimal("99") : new BigDecimal("12.34"),true,"PAYMENT_SUCCEEDED","PAY_SUCCESS","SUCCEEDED","CREDITED","PROCESSED",null,null,null,false,true,
                mode == Mode.ISSUE_WITH_IDS ? 8001L : null, mode == Mode.ISSUE_WITH_IDS ? 8002L : null,
                mode == Mode.ISSUE_WITH_IDS ? 8003L : null, mode == Mode.ISSUE_WITH_IDS ? 8004L : null);
        }
        public Map<Long, ReconciliationPlatformSnapshot> prefetch(String tenant, String provider,
            List<PaymentReconciliationLine> lines) {
            prefetchCalls.incrementAndGet();
            Map<Long, ReconciliationPlatformSnapshot> result = new LinkedHashMap<>();
            for (PaymentReconciliationLine line : lines) result.put(line.getId(), snapshot(tenant, provider, line));
            return result;
        }
        public void forEachMissingProviderEventPage(String t,Long batchId,String p,Instant s,Instant n,
            java.util.function.Consumer<List<ReconciliationPlatformEventProjection>> consumer) {
            if (mode == Mode.MATCHED_WITH_PLATFORM_MISSING) consumer.accept(List.of(new ReconciliationPlatformEventProjection(
                77L,"missing-77","PAYMENT_SUCCEEDED","ps-platform","order-platform","USD",
                new BigDecimal("12.340000"),s,"PROCESSED",7001L,7002L,7003L,s)));
            if (mode == Mode.FAIL_AFTER_FIRST_ISSUE) consumer.accept(List.of(new ReconciliationPlatformEventProjection(999L,"bad","PAYMENT_SUCCEEDED",s)));
            if (mode == Mode.MANY_MISSING) {
                for (int offset : new int[]{0, 500, 1000}) {
                    int size = Math.min(500, 1003 - offset);
                    List<ReconciliationPlatformEventProjection> page = java.util.stream.IntStream.range(0, size)
                        .mapToObj(index -> new ReconciliationPlatformEventProjection((long) offset + index + 10_000,
                            "missing-" + (offset + index), "PAYMENT_SUCCEEDED", s.plusMillis(offset + index)))
                        .toList();
                    emittedPageSizes.add(page.size()); consumer.accept(page);
                }
            }
        }
    }

    @Configuration @EnableTransactionManagement @MapperScan(basePackageClasses = PaymentReconciliationBatchMapper.class)
    static class Config {
        @Bean DataSource dataSource() { JdbcDataSource ds=new JdbcDataSource(); ds.setURL("jdbc:h2:mem:execute_it;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"); return ds; }
        @Bean PlatformTransactionManager transactionManager(DataSource ds) { return new DataSourceTransactionManager(ds); }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource ds) throws Exception { SqlSessionFactoryBean b=new SqlSessionFactoryBean(); b.setDataSource(ds); org.apache.ibatis.session.Configuration c=new org.apache.ibatis.session.Configuration(); c.setMapUnderscoreToCamelCase(true); b.setConfiguration(c); return b.getObject(); }
        @Bean JdbcTemplate jdbcTemplate(DataSource ds) { return new JdbcTemplate(ds); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper().findAndRegisterModules(); }
        @Bean PaymentReconciliationMatcher matcher(ObjectMapper o) { return new PaymentReconciliationMatcher(o); }
        @Bean TestPlatformDataSource platformDataSource() { return new TestPlatformDataSource(); }
        @Bean PaymentReconciliationOperatorProvider operatorProvider() { return new PaymentReconciliationOperatorProvider(){ public Operator current(){ return new Operator(9L,"operator"); }}; }
        @Bean PaymentReconciliationFailureRecorder failureRecorder(PaymentReconciliationBatchMapper b,PaymentReconciliationActionLogMapper l){ return new PaymentReconciliationFailureRecorder(b,l); }
        @Bean PaymentReconciliationExecutionService execution(PaymentReconciliationBatchMapper b,PaymentReconciliationLineMapper l,PaymentReconciliationIssueMapper i,PaymentReconciliationActionLogMapper a,PaymentReconciliationMatcher m,TestPlatformDataSource p,PaymentReconciliationOperatorProvider o,PaymentReconciliationFailureRecorder f,ObjectProvider<PaymentReconciliationExecutionService> self,ObjectMapper json){ return new PaymentReconciliationExecutionService(b,l,i,a,m,p,o,f,self,json); }
    }
}
