package com.gameluck.payment.domain.vo;

import com.gameluck.payment.domain.PurchaseReversal;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Purchase asset reversal admin view object.
 */
@Data
@AutoMapper(target = PurchaseReversal.class)
public class PurchaseReversalVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String reversalNo;

    private String reversalType;

    private String status;

    private String reason;

    private String reviewReason;

    private Date completedTime;

    private List<PurchaseReversalItemVo> items = new ArrayList<>();
}
