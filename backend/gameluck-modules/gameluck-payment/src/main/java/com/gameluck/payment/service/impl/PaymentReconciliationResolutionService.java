package com.gameluck.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.payment.domain.PaymentReconciliationActionLog;
import com.gameluck.payment.domain.PaymentReconciliationIssue;
import com.gameluck.payment.domain.bo.PaymentReconciliationResolutionBo;
import com.gameluck.payment.enums.PaymentReconciliationResolutionType;
import com.gameluck.payment.mapper.PaymentReconciliationActionLogMapper;
import com.gameluck.payment.mapper.PaymentReconciliationIssueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Locale;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class PaymentReconciliationResolutionService {
    public static final int STATE_CONFLICT_CODE = 40901;
    private static final int MAX_REMARK_LENGTH = 500;
    private final PaymentReconciliationIssueMapper issueMapper;
    private final PaymentReconciliationActionLogMapper actionLogMapper;
    private final Supplier<String> tenantProvider;
    private final Supplier<PaymentReconciliationOperatorProvider.Operator> operatorSupplier;

    @org.springframework.beans.factory.annotation.Autowired
    public PaymentReconciliationResolutionService(PaymentReconciliationIssueMapper issueMapper,
            PaymentReconciliationActionLogMapper actionLogMapper,
            PaymentReconciliationOperatorProvider operatorProvider) {
        this(issueMapper, actionLogMapper,
            com.gameluck.common.tenant.helper.TenantHelper::getTenantId, operatorProvider::current);
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentReconciliationIssue resolve(Long issueId, PaymentReconciliationResolutionBo bo) {
        String resolutionType = resolutionType(bo);
        return mutate(issueId, "RESOLVED", resolutionType, remark(bo), expectedVersion(bo), "RESOLVE");
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentReconciliationIssue ignore(Long issueId, PaymentReconciliationResolutionBo bo) {
        String resolutionType = resolutionType(bo);
        return mutate(issueId, "IGNORED", resolutionType, remark(bo), expectedVersion(bo), "IGNORE");
    }

    private PaymentReconciliationIssue mutate(Long issueId, String nextStatus, String resolutionType,
                                               String remark, Integer expectedVersion, String actionType) {
        String tenantId = tenantProvider.get();
        PaymentReconciliationIssue issue = issueMapper.selectByTenantAndId(tenantId, issueId);
        if (issue == null) throw error("payment.reconciliation.issue.notFound");
        if (!"OPEN".equals(issue.getStatus())) throw conflict();
        PaymentReconciliationOperatorProvider.Operator operator = operatorSupplier.get();
        if (operator == null || operator.id() == null || operator.name() == null || operator.name().trim().isEmpty()) {
            throw error("payment.reconciliation.operator.required");
        }
        Date now = new Date();
        int updated = issueMapper.resolveOpenIssue(tenantId, issueId, expectedVersion, nextStatus,
            resolutionType, remark, operator.id(), now);
        if (updated != 1) throw conflict();
        PaymentReconciliationActionLog log = new PaymentReconciliationActionLog();
        log.setId(IdUtil.getSnowflakeNextId()); log.setTenantId(tenantId); log.setBatchId(issue.getBatchId());
        log.setIssueId(issueId); log.setActionType(actionType); log.setBeforeStatus("OPEN");
        log.setAfterStatus(nextStatus); log.setOperatorId(operator.id()); log.setOperatorName(operator.name().trim());
        log.setRemark(remark); log.setCreateTime(now);
        actionLogMapper.insert(log);
        issue.setStatus(nextStatus); issue.setResolutionType(resolutionType); issue.setResolutionRemark(remark);
        issue.setResolvedBy(operator.id()); issue.setResolvedTime(now);
        return issue;
    }

    private String resolutionType(PaymentReconciliationResolutionBo bo) {
        String value = bo == null || bo.getResolutionType() == null ? "" : bo.getResolutionType().trim().toUpperCase(Locale.ROOT);
        try { return PaymentReconciliationResolutionType.valueOf(value).name(); }
        catch (IllegalArgumentException exception) { throw error("payment.reconciliation.resolution.type.invalid"); }
    }

    private String remark(PaymentReconciliationResolutionBo bo) {
        String value = bo == null || bo.getRemark() == null ? "" : bo.getRemark().trim();
        if (value.isEmpty()) throw error("payment.reconciliation.resolution.remark.required");
        if (value.length() > MAX_REMARK_LENGTH) throw error("payment.reconciliation.resolution.remark.tooLong");
        return value;
    }

    private Integer expectedVersion(PaymentReconciliationResolutionBo bo) {
        Integer value = bo == null ? null : bo.getExpectedVersion();
        if (value == null || value < 0) throw conflict();
        return value;
    }

    private ServiceException error(String key) { return new ServiceException(MessageUtils.message(key)); }
    private ServiceException conflict() {
        return new ServiceException(MessageUtils.message("payment.reconciliation.issue.stateConflict"), STATE_CONFLICT_CODE);
    }
}
