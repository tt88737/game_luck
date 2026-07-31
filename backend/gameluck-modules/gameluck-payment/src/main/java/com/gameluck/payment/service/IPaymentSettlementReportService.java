package com.gameluck.payment.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.payment.domain.bo.PaymentSettlementReportQueryBo;
import com.gameluck.payment.domain.vo.PaymentSettlementBatchVo;
import com.gameluck.payment.domain.vo.PaymentSettlementReportPageVo;

import java.time.LocalDate;
import java.util.List;

public interface IPaymentSettlementReportService {
    PaymentSettlementReportPageVo queryPage(PaymentSettlementReportQueryBo bo, PageQuery pageQuery);
    List<PaymentSettlementBatchVo> queryBatches(LocalDate date, String providerCode, String currencyCode);
    byte[] export(PaymentSettlementReportQueryBo bo);
}
