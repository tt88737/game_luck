package com.gameluck.wallet.domain.bo;

import com.gameluck.wallet.domain.WalletRelease;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.util.Date;

/**
 * Wallet release query business object.
 */
@Data
@AutoMapper(target = WalletRelease.class, reverseConvertGenerate = false)
public class WalletReleaseBo {

    private Long id;

    private String tenantId;

    private String releaseNo;

    private Long memberId;

    private String currencyCode;

    private String sourceType;

    private String businessNo;

    private String releaseMode;

    private String releaseStatus;

    private Date beginTime;

    private Date endTime;
}
