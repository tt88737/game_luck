package com.gameluck.wallet.service.impl;

import com.gameluck.common.core.constant.SystemConstants;
import com.gameluck.wallet.domain.WalletRule;
import com.gameluck.wallet.domain.vo.WalletRuleTemplateVo;
import com.gameluck.wallet.mapper.WalletRuleMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletRuleServiceImplTest {

    @Test
    @Tag("local")
    void previewMissingDefaultRulesMarksExistingAndDoesNotInsert() {
        WalletRuleMapper mapper = mock(WalletRuleMapper.class);
        WalletRuleServiceImpl service = new WalletRuleServiceImpl(mapper);
        when(mapper.selectList(any())).thenReturn(List.of(existingRule("tenant-a", "GC", "GAME_PAYOUT")));

        List<WalletRuleTemplateVo> templates = service.previewMissingDefaultRules("tenant-a");

        WalletRuleTemplateVo gameProfit = findTemplate(templates, "GC", "GAME_PROFIT");
        assertTrue(gameProfit.getExists());
        assertFalse(gameProfit.getWillCreate());
        WalletRuleTemplateVo scGameProfit = findTemplate(templates, "SC", "GAME_PROFIT");
        assertFalse(scGameProfit.getExists());
        assertTrue(scGameProfit.getWillCreate());
        verify(mapper, never()).insert(any(WalletRule.class));
    }

    @Test
    @Tag("local")
    void seedMissingDefaultRulesInsertsOnlyMissingRules() {
        WalletRuleMapper mapper = mock(WalletRuleMapper.class);
        WalletRuleServiceImpl service = new WalletRuleServiceImpl(mapper);
        when(mapper.selectList(any())).thenReturn(List.of(existingRule("000000", "GC", "GAME_PAYOUT")));
        when(mapper.insert(any(WalletRule.class))).thenReturn(1);

        int inserted = service.seedMissingDefaultRules(null);

        int expected = service.listDefaultTemplates().size() - 1;
        assertEquals(expected, inserted);
        ArgumentCaptor<WalletRule> captor = ArgumentCaptor.forClass(WalletRule.class);
        verify(mapper, org.mockito.Mockito.times(expected)).insert(captor.capture());
        assertTrue(captor.getAllValues().stream()
            .noneMatch(rule -> "GC".equals(rule.getCurrencyCode()) && "GAME_PROFIT".equals(rule.getSourceType())));
        WalletRule first = captor.getAllValues().get(0);
        assertNotNull(first.getId());
        assertEquals("000000", first.getTenantId());
        assertEquals(0, first.getVersion());
        assertEquals(SystemConstants.NORMAL, first.getDelFlag());
        assertNotNull(first.getCreateTime());
        assertNotNull(first.getUpdateTime());
    }

    @Test
    @Tag("local")
    void canonicalSourceTypeMapsLegacyAliases() {
        WalletRuleServiceImpl service = new WalletRuleServiceImpl(mock(WalletRuleMapper.class));

        assertEquals("GAME_PROFIT", service.canonicalSourceType("GAME_PAYOUT"));
        assertEquals("MANUAL_ADJUST", service.canonicalSourceType("ADJUST"));
        assertEquals("MANUAL_ADJUST", service.canonicalSourceType("ADJUSTMENT"));
        assertEquals("PROMOTION", service.canonicalSourceType("PROMOTION"));
    }

    @Test
    @Tag("local")
    void listDefaultTemplatesUsesExplicitSpecValues() {
        WalletRuleServiceImpl service = new WalletRuleServiceImpl(mock(WalletRuleMapper.class));

        List<WalletRuleTemplateVo> templates = service.listDefaultTemplates();

        assertTemplate(templates, "GC", "REGISTER_BONUS", "GC registration bonus", "0", "1", "1", "1", "IMMEDIATE", "1", "0");
        assertTemplate(templates, "SC", "REGISTER_BONUS", "SC registration bonus", "0", "1", "1", "1", "IMMEDIATE", "1", "0");
        assertTemplate(templates, "GC", "DAILY_REWARD", "GC daily login reward", "0", "1", "1", "1", "IMMEDIATE", "1", "0");
        assertTemplate(templates, "SC", "DAILY_REWARD", "SC daily login reward", "0", "1", "0", "1", "IMMEDIATE", "1", "0");
        assertTemplate(templates, "GC", "PROMOTION", "GC promotion", "0", "1", "1", "1", "IMMEDIATE", "1", "0");
        assertTemplate(templates, "SC", "PROMOTION", "SC promotion", "0", "1", "1", "1", "AFTER_TURNOVER", "0", "0");
        assertTemplate(templates, "RC", "DEPOSIT", "RC deposit", "0", "0", "0", "1", "IMMEDIATE", "1", "0");
        assertTemplate(templates, "GC", "GAME_PROFIT", "GC game profit", "0", "0", "1", "1", "NEVER", "1", "0");
        assertTemplate(templates, "SC", "GAME_PROFIT", "SC game profit", "0", "0", "1", "0", "AFTER_TURNOVER", "0", "0");
        assertTemplate(templates, "SC", "GAME_REFUND", "SC game refund", "0", "0", "1", "0", "IMMEDIATE", "0", "0");
        assertTemplate(templates, "GC", "MANUAL_ADJUST", "GC manual adjustment", "0", "0", "1", "1", "IMMEDIATE", "1", "0");
        assertTemplate(templates, "SC", "MANUAL_ADJUST", "SC manual adjustment", "0", "0", "1", "0", "MANUAL_REVIEW", "1", "0");
        assertTemplate(templates, "RC", "MANUAL_ADJUST", "RC manual adjustment", "0", "0", "0", "1", "MANUAL_REVIEW", "1", "0");
    }

    private static WalletRule existingRule(String tenantId, String currencyCode, String sourceType) {
        WalletRule rule = new WalletRule();
        rule.setTenantId(tenantId);
        rule.setCurrencyCode(currencyCode);
        rule.setSourceType(sourceType);
        return rule;
    }

    private static WalletRuleTemplateVo findTemplate(List<WalletRuleTemplateVo> templates, String currencyCode, String sourceType) {
        return templates.stream()
            .filter(template -> currencyCode.equals(template.getCurrencyCode())
                && sourceType.equals(template.getSourceType()))
            .findFirst()
            .orElseThrow();
    }

    private static void assertTemplate(List<WalletRuleTemplateVo> templates, String currencyCode, String sourceType,
                                       String ruleName, String creditEnabled, String debitEnabled,
                                       String withdrawEnabled, String exchangeEnabled, String releaseMode,
                                       String turnoverRequired, String defaultRequiredTurnover) {
        WalletRuleTemplateVo template = findTemplate(templates, currencyCode, sourceType);
        assertEquals(ruleName, template.getRuleName());
        assertEquals(creditEnabled, template.getCreditEnabled());
        assertEquals(debitEnabled, template.getDebitEnabled());
        assertEquals(withdrawEnabled, template.getWithdrawEnabled());
        assertEquals(exchangeEnabled, template.getExchangeEnabled());
        assertEquals(releaseMode, template.getReleaseMode());
        assertEquals(turnoverRequired, template.getTurnoverRequired());
        assertEquals(0, new BigDecimal(defaultRequiredTurnover).compareTo(template.getDefaultRequiredTurnover()));
    }
}
