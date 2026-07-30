package com.gameluck.payment.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Purchase order detail with grant snapshots and payment events.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseOrderDetailVo extends PurchaseOrderVo {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<PurchaseOrderGrantSnapshotVo> grantSnapshots = new ArrayList<>();

    private List<PurchasePaymentEventVo> paymentEvents = new ArrayList<>();

    private PurchaseReversalVo reversal;
}
