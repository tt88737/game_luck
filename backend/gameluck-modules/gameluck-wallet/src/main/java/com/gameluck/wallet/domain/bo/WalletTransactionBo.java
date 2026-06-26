package com.gameluck.wallet.domain.bo;

import com.gameluck.wallet.domain.WalletTransaction;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.util.Date;

/**
 * Wallet transaction query business object.
 */
@Data
@AutoMapper(target = WalletTransaction.class, reverseConvertGenerate = false)
public class WalletTransactionBo {

    private Long id;

    private String tenantId;

    private String transactionNo;

    private Long memberId;

    private String currencyCode;

    private String operation;

    private String sourceType;

    private String businessNo;

    private String status;

    private Date beginTime;

    private Date endTime;
}
