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

    @NotBlank(message = "币种不能为空")
    private String currencyCode;

    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    private String creditEnabled;

    private String debitEnabled;

    private String withdrawEnabled;

    private String exchangeEnabled;

    @NotBlank(message = "释放模式不能为空")
    private String releaseMode;

    private String turnoverRequired;

    @DecimalMin(value = "0", message = "默认所需流水不能小于0")
    private BigDecimal defaultRequiredTurnover;

    private String status;

    private Integer sortOrder;

    private String remark;
}
