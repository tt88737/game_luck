package com.gameluck.wallet.service.impl;

import com.gameluck.common.core.constant.SystemConstants;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.wallet.domain.WalletRule;
import com.gameluck.wallet.domain.vo.WalletRuleTemplateVo;
import com.gameluck.wallet.domain.vo.WalletRuleVo;
import com.gameluck.wallet.mapper.WalletRuleMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void resolveCreditRulePrefersCanonicalRuleWhenCanonicalAndAliasCoexist() {
        WalletRuleMapper mapper = mock(WalletRuleMapper.class);
        WalletRuleServiceImpl service = new WalletRuleServiceImpl(mapper);
        when(mapper.selectList(any())).thenReturn(List.of(
            creditRule("tenant-a", "GC", "GAME_PAYOUT", SystemConstants.NORMAL),
            creditRule("tenant-a", "GC", "GAME_PROFIT", SystemConstants.DISABLE)
        ));

        assertThrows(ServiceException.class, () -> service.resolveCreditRule("tenant-a", "GC", "GAME_PROFIT"));
    }

    @Test
    @Tag("local")
    void resolveCreditRulePrefersCanonicalRuleForAliasRequest() {
        WalletRuleMapper mapper = mock(WalletRuleMapper.class);
        WalletRuleServiceImpl service = new WalletRuleServiceImpl(mapper);
        when(mapper.selectList(any())).thenReturn(List.of(
            creditRule("tenant-a", "GC", "GAME_PAYOUT", SystemConstants.DISABLE),
            creditRule("tenant-a", "GC", "GAME_PROFIT", SystemConstants.NORMAL)
        ));

        WalletRuleVo rule = service.resolveCreditRule("tenant-a", "GC", "GAME_PAYOUT");

        assertEquals("GAME_PROFIT", rule.getSourceType());
    }

    @Test
    @Tag("local")
    void seedMissingDefaultRulesSkipsDuplicateKeyException() {
        WalletRuleMapper mapper = mock(WalletRuleMapper.class);
        WalletRuleServiceImpl service = new WalletRuleServiceImpl(mapper);
        when(mapper.selectList(any())).thenReturn(List.of());
        when(mapper.insert(any(WalletRule.class))).thenThrow(new DuplicateKeyException("duplicate"));

        int inserted = service.seedMissingDefaultRules("tenant-a");

        assertEquals(0, inserted);
    }

    @Test
    @Tag("local")
    void listDefaultTemplatesUsesExplicitSpecValues() {
        WalletRuleServiceImpl service = new WalletRuleServiceImpl(mock(WalletRuleMapper.class));

        List<WalletRuleTemplateVo> templates = service.listDefaultTemplates();

        assertEquals(13, templates.size());
        assertTemplate(templates, "GC", "REGISTER_BONUS", "Registration bonus", "GC registration bonus", "0", "1", "1", "1", "IMMEDIATE", "1", "0", 21, "Registration GC reward.");
        assertTemplate(templates, "SC", "REGISTER_BONUS", "Registration bonus", "SC registration bonus", "0", "1", "1", "1", "IMMEDIATE", "1", "0", 22, "Registration SC reward.");
        assertTemplate(templates, "GC", "DAILY_REWARD", "Daily login reward", "GC daily login reward", "0", "1", "1", "1", "IMMEDIATE", "1", "0", 31, "Daily login GC reward.");
        assertTemplate(templates, "SC", "DAILY_REWARD", "Daily login reward", "SC daily login reward", "0", "1", "0", "1", "IMMEDIATE", "1", "0", 32, "Daily login SC reward.");
        assertTemplate(templates, "GC", "PROMOTION", "Promotion", "GC promotion", "0", "1", "1", "1", "IMMEDIATE", "1", "0", 41, "Promotion GC reward.");
        assertTemplate(templates, "SC", "PROMOTION", "Promotion", "SC promotion", "0", "1", "1", "1", "AFTER_TURNOVER", "0", "0", 42, "Promotion SC reward after turnover.");
        assertTemplate(templates, "RC", "DEPOSIT", "Deposit", "RC deposit", "0", "0", "0", "1", "IMMEDIATE", "1", "0", 51, "RC deposit can be withdrawable immediately.");
        assertTemplate(templates, "GC", "GAME_PROFIT", "Game profit", "GC game profit", "0", "0", "1", "1", "NEVER", "1", "0", 61, "GC is not withdrawable or exchangeable.");
        assertTemplate(templates, "SC", "GAME_PROFIT", "Game profit", "SC game profit", "0", "0", "1", "0", "AFTER_TURNOVER", "0", "0", 62, "SC game profit can be exchanged after conditions.");
        assertTemplate(templates, "SC", "GAME_REFUND", "Game refund", "SC game refund", "0", "0", "1", "0", "IMMEDIATE", "0", "0", 71, "SC refund returns original stake immediately.");
        assertTemplate(templates, "GC", "MANUAL_ADJUST", "Manual adjustment", "GC manual adjustment", "0", "0", "1", "1", "IMMEDIATE", "1", "0", 81, "Manual GC adjustment.");
        assertTemplate(templates, "SC", "MANUAL_ADJUST", "Manual adjustment", "SC manual adjustment", "0", "0", "1", "0", "MANUAL_REVIEW", "1", "0", 82, "Manual SC adjustment uses operation strategy.");
        assertTemplate(templates, "RC", "MANUAL_ADJUST", "Manual adjustment", "RC manual adjustment", "0", "0", "0", "1", "MANUAL_REVIEW", "1", "0", 83, "Manual RC adjustment uses operation strategy.");
    }

    private static WalletRule existingRule(String tenantId, String currencyCode, String sourceType) {
        WalletRule rule = new WalletRule();
        rule.setTenantId(tenantId);
        rule.setCurrencyCode(currencyCode);
        rule.setSourceType(sourceType);
        return rule;
    }

    private static WalletRule creditRule(String tenantId, String currencyCode, String sourceType, String status) {
        WalletRule rule = existingRule(tenantId, currencyCode, sourceType);
        rule.setStatus(status);
        rule.setCreditEnabled(SystemConstants.NORMAL);
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
                                       String sourceLabel, String ruleName, String creditEnabled, String debitEnabled,
                                       String withdrawEnabled, String exchangeEnabled, String releaseMode,
                                       String turnoverRequired, String defaultRequiredTurnover, Integer sortOrder,
                                       String remark) {
        WalletRuleTemplateVo template = findTemplate(templates, currencyCode, sourceType);
        assertEquals(sourceLabel, template.getSourceLabel());
        assertEquals(ruleName, template.getRuleName());
        assertEquals(creditEnabled, template.getCreditEnabled());
        assertEquals(debitEnabled, template.getDebitEnabled());
        assertEquals(withdrawEnabled, template.getWithdrawEnabled());
        assertEquals(exchangeEnabled, template.getExchangeEnabled());
        assertEquals(releaseMode, template.getReleaseMode());
        assertEquals(turnoverRequired, template.getTurnoverRequired());
        assertEquals(0, new BigDecimal(defaultRequiredTurnover).compareTo(template.getDefaultRequiredTurnover()));
        assertEquals(SystemConstants.NORMAL, template.getStatus());
        assertEquals(sortOrder, template.getSortOrder());
        assertEquals(remark, template.getRemark());
    }
}
