package com.gameluck.payment.service.impl;

import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.domain.bo.PaymentSettlementPayoutCommandBo;
import com.gameluck.payment.mapper.PaymentSettlementBatchMapper;
import com.gameluck.payment.mapper.PaymentSettlementPayoutActionLogMapper;
import com.gameluck.payment.mapper.PaymentSettlementPayoutMapper;
import com.gameluck.payment.service.IPaymentSettlementPayoutService;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
@ContextConfiguration(classes = PaymentSettlementPayoutTransactionIntegrationTest.Config.class)
@Tag("local")
class PaymentSettlementPayoutTransactionIntegrationTest {
    @Autowired IPaymentSettlementPayoutService service;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void schema() {
        jdbc.execute("drop table if exists gl_payment_settlement_payout_action_log");
        jdbc.execute("drop table if exists gl_payment_settlement_payout");
        jdbc.execute("create table gl_payment_settlement_payout (id bigint primary key, tenant_id varchar(64) not null, payout_no varchar(64) not null, settlement_batch_id bigint not null, settlement_no varchar(64) not null, provider_code varchar(32) not null, currency_code varchar(3) not null, payout_amount decimal(20,6) not null, settlement_evidence_json clob not null, payout_purpose varchar(500) not null, payee_reference varchar(128) not null, status varchar(32) not null, maker_id bigint not null, maker_name varchar(100) not null, submitter_id bigint, submitter_name varchar(100), reviewer_id bigint, reviewer_name varchar(100), decision_reason varchar(500), version int not null, submitted_time timestamp, reviewed_time timestamp, create_time timestamp, update_time timestamp)");
        jdbc.execute("create table gl_payment_settlement_payout_action_log (id bigint primary key, tenant_id varchar(64) not null, payout_id bigint not null, action_type varchar(32) not null, before_status varchar(32), after_status varchar(32), operator_id bigint not null, operator_name varchar(1) not null, reason varchar(500), evidence_snapshot_json clob, expected_version int, result_version int, create_time timestamp)");
        jdbc.update("insert into gl_payment_settlement_payout (id,tenant_id,payout_no,settlement_batch_id,settlement_no,provider_code,currency_code,payout_amount,settlement_evidence_json,payout_purpose,payee_reference,status,maker_id,maker_name,version,create_time,update_time) values (71,'000000','PSP71',41,'SET-41','SIMULATED','USD',12.340000,'{}','purpose','reference','DRAFT',100,'maker',2,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
    }

    @Test
    void actionInsertFailureRollsBackStateAndActionTogether() {
        PaymentSettlementPayoutCommandBo command = new PaymentSettlementPayoutCommandBo(); command.setVersion(2);
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("000000");
            assertThrows(RuntimeException.class, () -> service.submit(71L, command));
        }
        assertEquals("DRAFT", jdbc.queryForObject("select status from gl_payment_settlement_payout where id=71", String.class));
        assertEquals(2, jdbc.queryForObject("select version from gl_payment_settlement_payout where id=71", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from gl_payment_settlement_payout_action_log where payout_id=71", Integer.class));
    }

    @Configuration
    @EnableTransactionManagement
    @MapperScan(basePackageClasses = PaymentSettlementPayoutMapper.class)
    static class Config {
        @Bean DataSource dataSource() {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setURL("jdbc:h2:mem:settlement-payout;MODE=MySQL;DB_CLOSE_DELAY=-1"); return ds;
        }
        @Bean PlatformTransactionManager transactionManager(DataSource ds) { return new DataSourceTransactionManager(ds); }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource ds) throws Exception {
            SqlSessionFactoryBean bean = new SqlSessionFactoryBean(); bean.setDataSource(ds); return bean.getObject();
        }
        @Bean JdbcTemplate jdbcTemplate(DataSource ds) { return new JdbcTemplate(ds); }
        @Bean PaymentReconciliationOperatorProvider operatorProvider() {
            PaymentReconciliationOperatorProvider provider = mock(PaymentReconciliationOperatorProvider.class);
            when(provider.current()).thenReturn(new PaymentReconciliationOperatorProvider.Operator(100L, "maker"));
            return provider;
        }
        @Bean PaymentSettlementPayoutServiceImpl payoutService(PaymentSettlementBatchMapper batches,
                PaymentSettlementPayoutMapper payouts, PaymentSettlementPayoutActionLogMapper actions,
                PaymentReconciliationOperatorProvider operators) {
            return new PaymentSettlementPayoutServiceImpl(batches, payouts, actions, operators);
        }
    }
}
