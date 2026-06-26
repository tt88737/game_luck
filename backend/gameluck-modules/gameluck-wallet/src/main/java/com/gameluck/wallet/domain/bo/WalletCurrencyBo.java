package com.gameluck.wallet.domain.bo;

import com.gameluck.common.mybatis.core.domain.BaseEntity;
import com.gameluck.wallet.domain.WalletCurrency;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Wallet currency query business object.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = WalletCurrency.class, reverseConvertGenerate = false)
public class WalletCurrencyBo extends BaseEntity {

    private Long id;

    private String tenantId;

    private String currencyCode;

    private String currencyName;

    private String enabled;

    private String creditEnabled;

    private String debitEnabled;

    private String freezeEnabled;

    private String withdrawEnabled;

    private String exchangeEnabled;
}
