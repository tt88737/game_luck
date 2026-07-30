package com.gameluck.payment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.domain.PaymentWebhookEvent;
import com.gameluck.payment.domain.bo.PaymentSessionAdminBo;
import com.gameluck.payment.domain.bo.PaymentWebhookEventAdminBo;
import com.gameluck.payment.domain.vo.PaymentSessionAdminVo;
import com.gameluck.payment.domain.vo.PaymentWebhookEventAdminVo;
import com.gameluck.payment.domain.vo.PaymentWebhookEventDetailVo;
import com.gameluck.payment.domain.vo.PaymentWebhookRetryResultVo;
import com.gameluck.payment.enums.PaymentWebhookEventStatus;
import com.gameluck.payment.mapper.PaymentSessionMapper;
import com.gameluck.payment.mapper.PaymentWebhookEventMapper;
import com.gameluck.payment.service.IPaymentProviderAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentProviderAdminServiceImpl implements IPaymentProviderAdminService {
    private final PaymentSessionMapper sessionMapper;
    private final PaymentWebhookEventMapper eventMapper;
    private final PaymentWebhookBusinessProcessor businessProcessor;
    private final PaymentWebhookFailureRecorder failureRecorder;

    @Override
    public TableDataInfo<PaymentSessionAdminVo> querySessionPage(PaymentSessionAdminBo bo, PageQuery pageQuery) {
        PaymentSessionAdminBo query = bo == null ? new PaymentSessionAdminBo() : bo;
        Page<PaymentSessionAdminVo> page = sessionMapper.selectAdminPage(pageQuery.build(), TenantHelper.getTenantId(), query);
        return TableDataInfo.build(page);
    }

    @Override
    public PaymentSessionAdminVo querySessionById(Long id) {
        PaymentSessionAdminVo row = sessionMapper.selectAdminById(TenantHelper.getTenantId(), id);
        if (row == null) throw new ServiceException(MessageUtils.message("payment.session.not.exists"));
        return row;
    }

    @Override
    public TableDataInfo<PaymentWebhookEventAdminVo> queryWebhookPage(PaymentWebhookEventAdminBo bo, PageQuery pageQuery) {
        PaymentWebhookEventAdminBo query = bo == null ? new PaymentWebhookEventAdminBo() : bo;
        Page<PaymentWebhookEventAdminVo> page = eventMapper.selectAdminPage(pageQuery.build(), TenantHelper.getTenantId(), query);
        return TableDataInfo.build(page);
    }

    @Override
    public PaymentWebhookEventDetailVo queryWebhookById(Long id) {
        PaymentWebhookEventDetailVo row = eventMapper.selectAdminById(TenantHelper.getTenantId(), id);
        if (row == null) throw new ServiceException(MessageUtils.message("payment.webhook.event.not.exists"));
        return row;
    }

    @Override
    public PaymentWebhookRetryResultVo retryWebhookEvent(Long id) {
        return retryWebhookEvent(TenantHelper.getTenantId(), id);
    }

    PaymentWebhookRetryResultVo retryWebhookEvent(String tenantId, Long id) {
        PaymentWebhookEvent current = eventMapper.selectByIdForUpdate(tenantId, id);
        if (current == null) throw new ServiceException(MessageUtils.message("payment.webhook.event.not.exists"));
        if (!PaymentWebhookEventStatus.FAILED.name().equals(current.getStatus())) {
            throw new ServiceException(MessageUtils.message("payment.webhook.event.retry.status.invalid"));
        }
        PaymentWebhookBusinessProcessor.WebhookProcessingOutcome outcome;
        try {
            outcome = TenantHelper.dynamic(tenantId, () -> businessProcessor.processBusiness(id));
        } catch (RuntimeException exception) {
            PaymentWebhookEvent terminal = failureRecorder.record(tenantId, id);
            if (terminal != null) return toRetryResult(terminal);
            throw exception;
        }
        PaymentWebhookEventDetailVo result = eventMapper.selectAdminById(tenantId, id);
        if (result == null) {
            throw new ServiceException(MessageUtils.message("payment.webhook.event.retry.persistence.failed"));
        }
        return toRetryResult(result);
    }

    private PaymentWebhookRetryResultVo toRetryResult(PaymentWebhookEvent event) {
        PaymentWebhookRetryResultVo vo = new PaymentWebhookRetryResultVo();
        vo.setEventId(event.getId()); vo.setProviderEventId(event.getProviderEventId());
        vo.setStatus(event.getStatus()); vo.setFailureReason(event.getFailureReason());
        vo.setProcessingCount(event.getProcessingCount()); vo.setLastProcessingTime(event.getLastProcessingTime());
        return vo;
    }

    private PaymentWebhookRetryResultVo toRetryResult(PaymentWebhookEventAdminVo event) {
        PaymentWebhookRetryResultVo vo = new PaymentWebhookRetryResultVo();
        vo.setEventId(event.getId()); vo.setProviderEventId(event.getProviderEventId());
        vo.setStatus(event.getStatus()); vo.setFailureReason(event.getFailureReason());
        vo.setProcessingCount(event.getProcessingCount()); vo.setLastProcessingTime(event.getLastProcessingTime());
        return vo;
    }
}
