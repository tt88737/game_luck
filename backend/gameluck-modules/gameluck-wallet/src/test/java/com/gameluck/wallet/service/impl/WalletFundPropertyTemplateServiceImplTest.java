package com.gameluck.wallet.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.wallet.domain.WalletFundPropertyTemplate;
import com.gameluck.wallet.domain.bo.WalletFundPropertyTemplateBo;
import com.gameluck.wallet.domain.vo.WalletFundPropertyTemplateVo;
import com.gameluck.wallet.mapper.WalletFundPropertyTemplateMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletFundPropertyTemplateServiceImplTest {

    @Test
    @Tag("local")
    void fundPropertyTemplateDoesNotExposeReleaseModeConfiguration() {
        assertNoDefaultReleaseModeField(WalletFundPropertyTemplate.class);
        assertNoDefaultReleaseModeField(WalletFundPropertyTemplateBo.class);
        assertNoDefaultReleaseModeField(WalletFundPropertyTemplateVo.class);
    }

    @Test
    @Tag("local")
    void fundPropertyTemplateDoesNotExposeCurrencyCapabilityConfiguration() {
        assertNoField(WalletFundPropertyTemplate.class, "withdrawEnabled");
        assertNoField(WalletFundPropertyTemplateBo.class, "withdrawEnabled");
        assertNoField(WalletFundPropertyTemplateVo.class, "withdrawEnabled");
        assertNoField(WalletFundPropertyTemplate.class, "exchangeEnabled");
        assertNoField(WalletFundPropertyTemplateBo.class, "exchangeEnabled");
        assertNoField(WalletFundPropertyTemplateVo.class, "exchangeEnabled");
    }

    @Test
    @Tag("local")
    void insertStoresDisabledTemplateWithTurnoverDefaults() {
        WalletFundPropertyTemplateMapper mapper = mock(WalletFundPropertyTemplateMapper.class);
        when(mapper.insert(any(WalletFundPropertyTemplate.class))).thenReturn(1);
        WalletFundPropertyTemplateServiceImpl service = new WalletFundPropertyTemplateServiceImpl(mapper);
        WalletFundPropertyTemplateBo bo = validBo();

        int rows = service.insertByBo(bo);

        assertEquals(1, rows);
        ArgumentCaptor<WalletFundPropertyTemplate> captor = ArgumentCaptor.forClass(WalletFundPropertyTemplate.class);
        verify(mapper).insert(captor.capture());
        WalletFundPropertyTemplate template = captor.getValue();
        assertEquals("ACTIVITY_FREE_SPIN", template.getPropertyCode());
        assertEquals("MULTIPLIER", template.getDefaultTurnoverMode());
        assertEquals(new BigDecimal("0.000000"), template.getDefaultTurnoverRequiredAmount());
        assertEquals(new BigDecimal("8.0000"), template.getDefaultTurnoverMultiplier());
        assertEquals("ALL", template.getDefaultGameScopeType());
        assertEquals("1", template.getStatus());
    }

    @Test
    @Tag("local")
    void insertRejectsFixedTurnoverWithoutPositiveAmount() {
        WalletFundPropertyTemplateServiceImpl service = new WalletFundPropertyTemplateServiceImpl(mock(WalletFundPropertyTemplateMapper.class));
        WalletFundPropertyTemplateBo bo = validBo();
        bo.setDefaultTurnoverMode("FIXED");
        bo.setDefaultTurnoverRequiredAmount(BigDecimal.ZERO);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.insertByBo(bo));

        assertEquals("wallet.fund.property.turnover.amount.positive", exception.getMessage());
    }

    @Test
    @Tag("local")
    void updateRejectsMultiplierModeWithoutPositiveMultiplier() {
        WalletFundPropertyTemplateServiceImpl service = new WalletFundPropertyTemplateServiceImpl(mock(WalletFundPropertyTemplateMapper.class));
        WalletFundPropertyTemplateBo bo = validBo();
        bo.setId(100L);
        bo.setDefaultTurnoverMultiplier(BigDecimal.ZERO);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.updateByBo(bo));

        assertEquals("wallet.fund.property.turnover.multiplier.positive", exception.getMessage());
    }

    private WalletFundPropertyTemplateBo validBo() {
        WalletFundPropertyTemplateBo bo = new WalletFundPropertyTemplateBo();
        bo.setPropertyCode("ACTIVITY_FREE_SPIN");
        bo.setPropertyName("Activity free spin");
        bo.setDefaultSourceType("PROMOTION");
        bo.setDefaultTurnoverMode("MULTIPLIER");
        bo.setDefaultTurnoverMultiplier(new BigDecimal("8"));
        bo.setDefaultGameScopeType("ALL");
        return bo;
    }

    private void assertNoDefaultReleaseModeField(Class<?> type) {
        boolean hasField = Arrays.stream(type.getDeclaredFields())
            .anyMatch(field -> "defaultReleaseMode".equals(field.getName()));
        assertEquals(false, hasField);
    }

    private void assertNoField(Class<?> type, String fieldName) {
        boolean hasField = Arrays.stream(type.getDeclaredFields())
            .anyMatch(field -> fieldName.equals(field.getName()));
        assertEquals(false, hasField);
    }
}
