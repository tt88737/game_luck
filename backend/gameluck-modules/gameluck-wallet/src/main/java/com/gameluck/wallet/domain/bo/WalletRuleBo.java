package com.gameluck.wallet.domain.bo;

import com.gameluck.common.mybatis.core.domain.BaseEntity;
import com.gameluck.wallet.domain.WalletRule;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Wallet source rule business object.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = WalletRule.class, reverseConvertGenerate = false)
public class WalletRuleBo extends BaseEntity {

    private Long id;

    private String tenantId;

    @NotBlank(message = "{wallet.currency.required}")
    private String currencyCode;

    @NotBlank(message = "{wallet.source.type.required}")
    private String sourceType;

    @NotBlank(message = "{wallet.rule.name.required}")
    private String ruleName;

    private String creditEnabled;

    private String debitEnabled;

    private String withdrawEnabled;

    private String exchangeEnabled;

    @NotBlank(message = "{wallet.release.mode.required}")
    private String releaseMode;

    private String turnoverRequired;

    @DecimalMin(value = "0", message = "{wallet.default.required.turnover.nonnegative}")
    private BigDecimal defaultRequiredTurnover;

    private String status;

    private Integer sortOrder;

    private String remark;
}
