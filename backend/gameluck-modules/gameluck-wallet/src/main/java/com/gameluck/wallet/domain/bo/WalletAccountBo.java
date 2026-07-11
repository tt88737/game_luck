package com.gameluck.wallet.domain.bo;

import com.gameluck.common.mybatis.core.domain.BaseEntity;
import com.gameluck.wallet.domain.WalletAccount;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Wallet account query business object.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = WalletAccount.class, reverseConvertGenerate = false)
public class WalletAccountBo extends BaseEntity {

    private Long id;

    private String tenantId;

    private Long memberId;

    private String memberNo;

    private String currencyCode;

    private String status;
}
