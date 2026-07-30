package com.gameluck.wallet.domain.bo;

import com.gameluck.wallet.domain.WalletExchangeOrder;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.util.Date;

/**
 * Wallet exchange order query business object.
 */
@Data
@AutoMapper(target = WalletExchangeOrder.class, reverseConvertGenerate = false)
public class WalletExchangeOrderBo {

    private Long id;

    private String tenantId;

    private String exchangeOrderNo;

    private Long memberId;

    private String memberNo;

    private Long exchangeRuleId;

    private String fromCurrencyCode;

    private String toCurrencyCode;

    private String debitTransactionNo;

    private String creditTransactionNo;

    private String turnoverTaskNo;

    private String status;

    private Date beginTime;

    private Date endTime;
}
