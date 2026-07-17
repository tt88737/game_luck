package com.gameluck.wallet.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.gameluck.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gl_wallet_fund_property_template")
public class WalletFundPropertyTemplate extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;
    private String tenantId;
    private String propertyCode;
    private String propertyName;
    private String defaultSourceType;
    private String defaultTurnoverMode;
    private BigDecimal defaultTurnoverRequiredAmount;
    private BigDecimal defaultTurnoverMultiplier;
    private String defaultGameScopeType;
    private String defaultGameScopeValue;
    private String status;
    private Integer sortOrder;
    private String remark;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
