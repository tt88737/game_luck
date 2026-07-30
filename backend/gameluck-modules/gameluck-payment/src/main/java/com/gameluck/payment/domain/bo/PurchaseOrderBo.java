package com.gameluck.payment.domain.bo;

import com.gameluck.payment.domain.PurchaseOrder;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.util.Date;

/**
 * Purchase order admin query and manual action object.
 */
@Data
@AutoMapper(target = PurchaseOrder.class, reverseConvertGenerate = false)
public class PurchaseOrderBo {

    private Long id;

    private String tenantId;

    private String purchaseOrderNo;

    private Long memberId;

    private String memberNo;

    private Long offerId;

    private String offerNo;

    private String status;

    private String providerCode;

    private String providerOrderNo;

    private String paymentSessionNo;

    private String idempotencyKey;

    private String reason;

    private Date beginTime;

    private Date endTime;
}
