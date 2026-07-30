package com.gameluck.payment.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.payment.domain.bo.PaymentSettlementCreateBo;
import com.gameluck.payment.domain.bo.PaymentSettlementQueryBo;
import com.gameluck.payment.domain.bo.PaymentSettlementCloseBo;
import com.gameluck.payment.domain.vo.PaymentSettlementBatchVo;
import com.gameluck.payment.domain.vo.PaymentSettlementDetailVo;
import com.gameluck.payment.domain.vo.PaymentSettlementItemVo;

public interface IPaymentSettlementService {
    PaymentSettlementDetailVo create(PaymentSettlementCreateBo bo);
    TableDataInfo<PaymentSettlementBatchVo> queryPage(PaymentSettlementQueryBo bo, PageQuery pageQuery);
    PaymentSettlementDetailVo queryDetail(Long batchId);
    TableDataInfo<PaymentSettlementItemVo> queryItems(Long batchId, String eventType, PageQuery pageQuery);
    PaymentSettlementDetailVo calculate(Long batchId);
    PaymentSettlementDetailVo close(Long batchId, PaymentSettlementCloseBo bo);
}
