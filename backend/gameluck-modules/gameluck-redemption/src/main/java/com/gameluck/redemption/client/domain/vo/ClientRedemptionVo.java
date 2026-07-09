package com.gameluck.redemption.client.domain.vo;

import lombok.Data;

@Data
public class ClientRedemptionVo {

    private Long orderId;

    private String orderNo;

    private String currencyCode;

    private String amount;

    private String status;

    private String walletFreezeNo;

    private String reviewRemark;

    private String createdAt;
}
