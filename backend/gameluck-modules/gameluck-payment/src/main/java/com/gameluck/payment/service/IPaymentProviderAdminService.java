package com.gameluck.payment.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.payment.domain.bo.PaymentSessionAdminBo;
import com.gameluck.payment.domain.bo.PaymentWebhookEventAdminBo;
import com.gameluck.payment.domain.vo.PaymentSessionAdminVo;
import com.gameluck.payment.domain.vo.PaymentWebhookEventAdminVo;
import com.gameluck.payment.domain.vo.PaymentWebhookEventDetailVo;
import com.gameluck.payment.domain.vo.PaymentWebhookRetryResultVo;

public interface IPaymentProviderAdminService {
    TableDataInfo<PaymentSessionAdminVo> querySessionPage(PaymentSessionAdminBo bo, PageQuery pageQuery);
    PaymentSessionAdminVo querySessionById(Long id);
    TableDataInfo<PaymentWebhookEventAdminVo> queryWebhookPage(PaymentWebhookEventAdminBo bo, PageQuery pageQuery);
    PaymentWebhookEventDetailVo queryWebhookById(Long id);
    PaymentWebhookRetryResultVo retryWebhookEvent(Long id);
}
