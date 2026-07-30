package com.gameluck.payment.domain.vo;

import com.gameluck.member.domain.vo.MemberProfileVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseReversalReviewDetailVo extends PurchaseReversalReviewVo {
    private PurchaseOrderVo purchaseOrder;
    private MemberProfileVo member;
    private List<PurchaseOrderGrantSnapshotVo> grantSnapshots = new ArrayList<>();
    private List<PurchasePaymentEventVo> paymentEvents = new ArrayList<>();
    private List<PurchaseReversalReviewLogVo> reviewLogs = new ArrayList<>();
}
