package com.gameluck.payment.client.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ClientPaymentSessionVo {
    private String sessionNo;
    private String orderNo;
    private String providerCode;
    private String providerSessionNo;
    private String payCurrencyCode;
    private BigDecimal payAmount;
    private String checkoutUrl;
    private String status;
    private Date expireTime;
    private Date completedTime;
}
