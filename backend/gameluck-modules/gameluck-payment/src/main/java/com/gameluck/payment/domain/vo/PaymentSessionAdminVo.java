package com.gameluck.payment.domain.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class PaymentSessionAdminVo {
    private Long id;
    private String sessionNo;
    private Long purchaseOrderId;
    private String purchaseOrderNo;
    private Long memberId;
    private String memberNo;
    private String providerCode;
    private String providerSessionNo;
    private String payCurrencyCode;
    private BigDecimal payAmount;
    private String checkoutUrl;
    private String status;
    private Date expireTime;
    private Date completedTime;
    private Date createTime;
    private Date updateTime;
}
