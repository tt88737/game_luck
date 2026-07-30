package com.gameluck.payment.domain.vo;

import com.gameluck.payment.domain.PurchasePaymentEvent;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Purchase payment event admin view object.
 */
@Data
@AutoMapper(target = PurchasePaymentEvent.class)
public class PurchasePaymentEventVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String tenantId;

    private String eventKey;

    private String purchaseOrderNo;

    private String providerCode;

    private String providerOrderNo;

    private String eventType;

    private String eventStatus;

    private String requestHash;

    private String requestBody;

    private String processResult;

    private Date processTime;

    private Date createTime;
}
