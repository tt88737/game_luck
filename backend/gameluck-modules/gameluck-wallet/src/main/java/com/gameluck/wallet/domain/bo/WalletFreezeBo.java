package com.gameluck.wallet.domain.bo;

import com.gameluck.wallet.domain.WalletFreeze;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.util.Date;

/**
 * Wallet freeze query business object.
 */
@Data
@AutoMapper(target = WalletFreeze.class, reverseConvertGenerate = false)
public class WalletFreezeBo {

    private Long id;

    private String tenantId;

    private String freezeNo;

    private Long memberId;

    private String currencyCode;

    private String sourceType;

    private String businessNo;

    private String status;

    private Date beginTime;

    private Date endTime;
}
