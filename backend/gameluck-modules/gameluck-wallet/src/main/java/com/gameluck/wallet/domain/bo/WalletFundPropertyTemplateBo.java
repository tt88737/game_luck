package com.gameluck.wallet.domain.bo;

import com.gameluck.common.mybatis.core.domain.BaseEntity;
import com.gameluck.wallet.domain.WalletFundPropertyTemplate;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = WalletFundPropertyTemplate.class, reverseConvertGenerate = false)
public class WalletFundPropertyTemplateBo extends BaseEntity {

    private Long id;
    private String tenantId;
    @NotBlank(message = "{wallet.fund.property.code.required}")
    private String propertyCode;
    @NotBlank(message = "{wallet.fund.property.name.required}")
    private String propertyName;
    @NotBlank(message = "{wallet.fund.property.source.required}")
    private String defaultSourceType;
    private String defaultTurnoverMode;
    private BigDecimal defaultTurnoverRequiredAmount;
    private BigDecimal defaultTurnoverMultiplier;
    private String defaultGameScopeType;
    private String defaultGameScopeValue;
    private String status;
    private Integer sortOrder;
    private String remark;
}
