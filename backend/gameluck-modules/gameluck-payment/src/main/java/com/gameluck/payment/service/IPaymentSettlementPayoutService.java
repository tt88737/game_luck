package com.gameluck.payment.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.payment.domain.bo.PaymentSettlementPayoutCreateBo;
import com.gameluck.payment.domain.bo.PaymentSettlementPayoutQueryBo;
import com.gameluck.payment.domain.vo.PaymentSettlementPayoutDetailVo;
import com.gameluck.payment.domain.vo.PaymentSettlementPayoutRowVo;

public interface IPaymentSettlementPayoutService {
    PaymentSettlementPayoutDetailVo create(PaymentSettlementPayoutCreateBo bo);
    TableDataInfo<PaymentSettlementPayoutRowVo> queryPage(PaymentSettlementPayoutQueryBo bo, PageQuery pageQuery);
    PaymentSettlementPayoutDetailVo queryDetail(Long payoutId);
}
