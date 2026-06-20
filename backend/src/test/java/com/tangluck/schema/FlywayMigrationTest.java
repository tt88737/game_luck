package com.tangluck.schema;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void applicationContextLoadsWithFlywayMigrations() {
    }

    @Test
    void createsP0ACoreTables() {
        var tableCount = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = schema()
                  and table_name in (
                    'users',
                    'user_consent_logs',
                    'compliance_regions',
                    'compliance_documents',
                    'wallet_accounts',
                    'wallet_ledger',
                    'promotion_campaigns',
                    'promotion_claims',
                    'promotion_reward_grants',
                    'daily_task_progress',
                    'coupon_codes',
                    'risk_events',
                    'audit_logs',
                    'lobby_cards'
                  )
                """, Integer.class);

        assertThat(tableCount).isEqualTo(14);
    }
}
