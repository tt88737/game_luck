package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("gl_payment_session")
public class PaymentSession {

    @TableId(value = "id")
    private Long id;

    private String tenantId;
    private String sessionNo;
    private Long purchaseOrderId;
    private String purchaseOrderNo;
    private Long memberId;
    private String providerCode;
    private String providerSessionNo;
    private String payCurrencyCode;
    private BigDecimal payAmount;
    private String checkoutUrl;
    private String status;
    private String requestKey;
    private Date expireTime;
    private Date completedTime;
    private Integer version;
    private Date createTime;
    private Date updateTime;
}
