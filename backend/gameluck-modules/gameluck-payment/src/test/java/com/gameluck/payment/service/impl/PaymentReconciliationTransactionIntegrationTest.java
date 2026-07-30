package com.gameluck.payment.service.impl;

import com.gameluck.payment.domain.PaymentReconciliationBatch;
import com.gameluck.payment.mapper.PaymentReconciliationBatchMapper;
import com.gameluck.payment.mapper.PaymentReconciliationLineMapper;
import com.gameluck.payment.mapper.PaymentReconciliationActionLogMapper;
import com.gameluck.payment.service.reconciliation.ReconciliationParseResult;
import com.gameluck.payment.service.reconciliation.ReconciliationParsedLine;
import com.gameluck.payment.service.reconciliation.PaymentReconciliationCsvParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig
@ContextConfiguration(classes = PaymentReconciliationTransactionIntegrationTest.Config.class)
class PaymentReconciliationTransactionIntegrationTest {
    @Autowired PaymentReconciliationBatchCreator creator;
    @Autowired PaymentReconciliationValidationWorker worker;
    @Autowired PaymentReconciliationFailureRecorder recorder;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void schema() {
        jdbc.execute("drop table if exists gl_payment_reconciliation_line");
        jdbc.execute("drop table if exists gl_payment_reconciliation_action_log");
        jdbc.execute("drop table if exists gl_payment_reconciliation_batch");
        jdbc.execute("create table gl_payment_reconciliation_batch (id bigint primary key, tenant_id varchar(64) not null, provider_code varchar(32) not null, statement_date date not null, original_file_name varchar(255) not null, file_digest varchar(64) not null, total_count int default 0, valid_count int default 0, invalid_count int default 0, matched_count int default 0, discrepancy_count int default 0, status varchar(32) not null, failure_reason varchar(255), creator_id bigint, creator_name varchar(64), version int default 0, create_time timestamp, update_time timestamp, unique(tenant_id,provider_code,file_digest))");
        jdbc.execute("create table gl_payment_reconciliation_line (id bigint primary key, tenant_id varchar(64) not null, batch_id bigint not null, source_row_number bigint not null, provider_record_id varchar(128) not null check(provider_record_id <> 'boom'), event_type varchar(64), provider_session_no varchar(128), purchase_order_no varchar(128), currency_code varchar(3), amount decimal(20,6), occurred_time timestamp, status varchar(32), parse_error varchar(128), raw_fields_json clob, create_time timestamp)");
        jdbc.execute("create table gl_payment_reconciliation_action_log (id bigint primary key, tenant_id varchar(64) not null, batch_id bigint not null, issue_id bigint, action_type varchar(32) not null, before_status varchar(32), after_status varchar(32), operator_id bigint not null, operator_name varchar(100) not null, remark varchar(500), create_time timestamp)");
    }

    @Test @Tag("local")
    void committedBatchSurvivesWorkerRollbackAndGuardedRecorderMarksFailed() throws Exception {
        PaymentReconciliationBatch batch = batch(7L, "digest-a");
        creator.create(batch);
        assertEquals("UPLOADED", jdbc.queryForObject("select status from gl_payment_reconciliation_batch where id=7", String.class));

        StringBuilder csv = new StringBuilder("provider_record_id,event_type,provider_session_no,purchase_order_no,pay_currency_code,pay_amount,occurred_time\n");
        IntStream.range(0, 501).forEach(i -> csv.append(i == 500 ? "boom" : "r" + i)
            .append(",PAYMENT_SUCCEEDED,s,o,USD,1.000000,2026-07-28T00:00:00Z\n"));
        Path path = Files.createTempFile("reconciliation-it-", ".csv"); Files.writeString(path, csv, StandardCharsets.UTF_8);
        batch.setFileDigest(sha256(path));
        try { assertThrows(RuntimeException.class, () -> worker.validate("tenant-a", batch, path, Files.size(path))); }
        finally { Files.deleteIfExists(path); }
        assertEquals(0, jdbc.queryForObject("select count(*) from gl_payment_reconciliation_line", Integer.class));

        recorder.record("tenant-a", 7L, "RECONCILIATION_FILE_PROCESSING_FAILED");
        assertEquals("FAILED", jdbc.queryForObject("select status from gl_payment_reconciliation_batch where id=7", String.class));
        assertEquals("RECONCILIATION_FILE_PROCESSING_FAILED", jdbc.queryForObject("select failure_reason from gl_payment_reconciliation_batch where id=7", String.class));
        recorder.record("wrong-tenant", 7L, "must-not-change");
        assertEquals("RECONCILIATION_FILE_PROCESSING_FAILED", jdbc.queryForObject("select failure_reason from gl_payment_reconciliation_batch where id=7", String.class));
    }

    @Test @Tag("local")
    void databaseUniqueDigestConstraintRejectsConcurrentWindow() {
        creator.create(batch(7L, "same"));
        assertThrows(RuntimeException.class, () -> creator.create(batch(8L, "same")));
        assertEquals(1, jdbc.queryForObject("select count(*) from gl_payment_reconciliation_batch", Integer.class));
    }

    @Test @Tag("local")
    void fileRejectionLeavesCommittedBatchUploadedWithNoLines() throws Exception {
        Path path = Files.createTempFile("reconciliation-invalid-", ".csv");
        Files.writeString(path, "bad-header\n", StandardCharsets.UTF_8);
        PaymentReconciliationBatch batch = batch(10L, sha256(path));
        creator.create(batch);
        try {
            PaymentReconciliationFileException error = assertThrows(PaymentReconciliationFileException.class,
                () -> worker.validate("tenant-a", batch, path, Files.size(path)));
            assertEquals("INVALID_HEADER", error.code());
        } finally { Files.deleteIfExists(path); }
        assertEquals("UPLOADED", jdbc.queryForObject("select status from gl_payment_reconciliation_batch where id=10", String.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from gl_payment_reconciliation_line where batch_id=10", Integer.class));
    }

    @Test @Tag("local")
    void tamperedSpoolRollsBackWithoutLinesAndCanBeMarkedFailed() throws Exception {
        Path path = Files.createTempFile("reconciliation-tamper-", ".csv");
        Files.writeString(path, "original", StandardCharsets.UTF_8);
        PaymentReconciliationBatch batch = batch(12L, sha256(path)); creator.create(batch);
        Files.writeString(path, "tampered", StandardCharsets.UTF_8);
        try { assertThrows(SecurityException.class, () -> worker.validate("tenant-a", batch, path, Files.size(path))); }
        finally { Files.deleteIfExists(path); }
        assertEquals(0, jdbc.queryForObject("select count(*) from gl_payment_reconciliation_line where batch_id=12", Integer.class));
        recorder.record("tenant-a", 12L, "RECONCILIATION_FILE_PROCESSING_FAILED");
        assertEquals("FAILED", jdbc.queryForObject("select status from gl_payment_reconciliation_batch where id=12", String.class));
    }

    @Test @Tag("local")
    void validationAndUploadActionLogCommitAtomically() throws Exception {
        Path path = validFile("atomic-success");
        PaymentReconciliationBatch batch = batch(20L, sha256(path)); creator.create(batch);
        try {
            worker.validate("tenant-a", batch, path, Files.size(path),
                new PaymentReconciliationValidationWorker.UploadAction(9L, "operator"));
        } finally { Files.deleteIfExists(path); }
        assertEquals("VALIDATED", jdbc.queryForObject("select status from gl_payment_reconciliation_batch where id=20", String.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from gl_payment_reconciliation_line where batch_id=20", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from gl_payment_reconciliation_action_log where batch_id=20 and action_type='UPLOAD'", Integer.class));
    }

    @Test @Tag("local")
    void uploadActionLogFailureRollsBackLinesAndValidationButKeepsUploadedBatch() throws Exception {
        jdbc.execute("drop table gl_payment_reconciliation_action_log");
        jdbc.execute("create table gl_payment_reconciliation_action_log (id bigint primary key, tenant_id varchar(64) not null, batch_id bigint not null, issue_id bigint, action_type varchar(32) not null, before_status varchar(32), after_status varchar(32), operator_id bigint not null, operator_name varchar(1) not null, remark varchar(500), create_time timestamp)");
        Path path = validFile("atomic-failure");
        PaymentReconciliationBatch batch = batch(21L, sha256(path)); creator.create(batch);
        try {
            assertThrows(RuntimeException.class, () -> worker.validate("tenant-a", batch, path, Files.size(path),
                new PaymentReconciliationValidationWorker.UploadAction(9L, "operator")));
        } finally { Files.deleteIfExists(path); }
        assertEquals("UPLOADED", jdbc.queryForObject("select status from gl_payment_reconciliation_batch where id=21", String.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from gl_payment_reconciliation_line where batch_id=21", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from gl_payment_reconciliation_action_log where batch_id=21", Integer.class));
    }

    private Path validFile(String prefix) throws Exception {
        Path path = Files.createTempFile(prefix, ".csv");
        Files.writeString(path, "provider_record_id,event_type,provider_session_no,purchase_order_no,pay_currency_code,pay_amount,occurred_time\n"
            + "r1,PAYMENT_SUCCEEDED,s,o,USD,1.000000,2026-07-28T00:00:00Z\n", StandardCharsets.UTF_8);
        return path;
    }

    private PaymentReconciliationBatch batch(long id, String digest) {
        PaymentReconciliationBatch b = new PaymentReconciliationBatch(); b.setId(id); b.setTenantId("tenant-a");
        b.setProviderCode("SIMULATED"); b.setStatementDate(java.sql.Date.valueOf("2026-07-28"));
        b.setOriginalFileName("statement.csv"); b.setFileDigest(digest); b.setStatus("UPLOADED"); return b;
    }
    private ReconciliationParsedLine line(int index, String providerId) {
        return new ReconciliationParsedLine(index + 2L, providerId, "PAYMENT_SUCCEEDED", "s", "o", "USD",
            new BigDecimal("1.000000"), Instant.EPOCH, "[]", ReconciliationParsedLine.Status.VALID, null);
    }
    private String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    @Configuration
    @EnableTransactionManagement
    @MapperScan(basePackageClasses = PaymentReconciliationBatchMapper.class)
    static class Config {
        @Bean DataSource dataSource() { JdbcDataSource ds = new JdbcDataSource(); ds.setURL("jdbc:h2:mem:reconciliation;MODE=MySQL;DB_CLOSE_DELAY=-1"); return ds; }
        @Bean PlatformTransactionManager transactionManager(DataSource ds) { return new DataSourceTransactionManager(ds); }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource ds) throws Exception { SqlSessionFactoryBean bean = new SqlSessionFactoryBean(); bean.setDataSource(ds); return bean.getObject(); }
        @Bean JdbcTemplate jdbcTemplate(DataSource ds) { return new JdbcTemplate(ds); }
        @Bean PaymentReconciliationBatchCreator creator(PaymentReconciliationBatchMapper mapper) { return new PaymentReconciliationBatchCreator(mapper); }
        @Bean PaymentReconciliationCsvParser parser() { return new PaymentReconciliationCsvParser(new ObjectMapper()); }
        @Bean PaymentReconciliationValidationWorker worker(PaymentReconciliationLineMapper lines, PaymentReconciliationBatchMapper batches, PaymentReconciliationActionLogMapper logs, PaymentReconciliationCsvParser parser) { return new PaymentReconciliationValidationWorker(lines, batches, logs, parser); }
        @Bean PaymentReconciliationFailureRecorder recorder(PaymentReconciliationBatchMapper mapper) { return new PaymentReconciliationFailureRecorder(mapper); }
    }
}
