package com.gameluck.wallet.domain.vo;

import com.gameluck.wallet.domain.WalletCurrency;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Wallet currency view object.
 */
@Data
@AutoMapper(target = WalletCurrency.class)
public class WalletCurrencyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String tenantId;

    private String currencyCode;

    private String currencyName;

    private Integer scaleNum;

    private String enabled;

    private String creditEnabled;

    private String debitEnabled;

    private String freezeEnabled;

    private String depositEnabled;

    private String withdrawEnabled;

    private String exchangeEnabled;

    private String exchangeInEnabled;

    private String exchangeOutEnabled;

    private String playEnabled;

    private String negativeAllowed;

    private Integer sortOrder;

    private String remark;

    private Date createTime;

    private Date updateTime;

    private Integer version;
}
