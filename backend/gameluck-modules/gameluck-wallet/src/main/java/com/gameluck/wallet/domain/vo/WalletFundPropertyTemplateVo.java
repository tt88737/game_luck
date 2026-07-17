package com.gameluck.wallet.domain.vo;

import com.gameluck.wallet.domain.WalletFundPropertyTemplate;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@AutoMapper(target = WalletFundPropertyTemplate.class)
public class WalletFundPropertyTemplateVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
    private Date createTime;
    private Date updateTime;
    private Integer version;
}
