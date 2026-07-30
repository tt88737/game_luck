package com.gameluck.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.domain.PaymentSettlementBatch;
import com.gameluck.payment.domain.PaymentSettlementPayout;
import com.gameluck.payment.domain.PaymentSettlementPayoutActionLog;
import com.gameluck.payment.domain.bo.PaymentSettlementPayoutCreateBo;
import com.gameluck.payment.domain.bo.PaymentSettlementPayoutCommandBo;
import com.gameluck.payment.domain.bo.PaymentSettlementPayoutEditBo;
import com.gameluck.payment.domain.bo.PaymentSettlementPayoutQueryBo;
import com.gameluck.payment.domain.vo.PaymentSettlementPayoutActionLogVo;
import com.gameluck.payment.domain.vo.PaymentSettlementPayoutDetailVo;
import com.gameluck.payment.domain.vo.PaymentSettlementPayoutRowVo;
import com.gameluck.payment.mapper.PaymentSettlementBatchMapper;
import com.gameluck.payment.mapper.PaymentSettlementPayoutActionLogMapper;
import com.gameluck.payment.mapper.PaymentSettlementPayoutMapper;
import com.gameluck.payment.service.IPaymentSettlementPayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentSettlementPayoutServiceImpl implements IPaymentSettlementPayoutService {
    private static final int PURPOSE_MAX_LENGTH = 500;
    private static final int PAYEE_REFERENCE_MAX_LENGTH = 128;
    private static final int REASON_MAX_LENGTH = 500;

    private final PaymentSettlementBatchMapper batchMapper;
    private final PaymentSettlementPayoutMapper payoutMapper;
    private final PaymentSettlementPayoutActionLogMapper actionLogMapper;
    private final PaymentReconciliationOperatorProvider operatorProvider;
    @Autowired(required = false)
    private PaymentSettlementPayoutApprovalService approvalService;

    @Override
    @Transactional
    public PaymentSettlementPayoutDetailVo create(PaymentSettlementPayoutCreateBo bo) {
        CreateInput input = validateCreate(bo);
        String tenantId = TenantHelper.getTenantId();
        PaymentSettlementBatch batch = batchMapper.selectByTenantAndId(tenantId, input.batchId());
        if (batch == null) throw new ServiceException("payment.settlementPayout.batch.notFound");
        if (!"CLOSED".equals(batch.getStatus())) {
            throw new ServiceException("payment.settlementPayout.status.ineligible");
        }
        if (batch.getNetSettlement() == null || batch.getNetSettlement().signum() <= 0) {
            throw new ServiceException("payment.settlementPayout.amount.ineligible");
        }
        if (payoutMapper.selectByTenantAndBatchId(tenantId, input.batchId()) != null) {
            throw new ServiceException("payment.settlementPayout.duplicate");
        }
        PaymentReconciliationOperatorProvider.Operator operator = operatorProvider.current();
        if (operator == null || operator.id() == null || blank(operator.name())) {
            throw new ServiceException("payment.settlementPayout.operator.required");
        }

        long id = IdUtil.getSnowflakeNextId();
        Date now = new Date();
        PaymentSettlementPayout payout = new PaymentSettlementPayout();
        payout.setId(id); payout.setTenantId(tenantId); payout.setPayoutNo("PSP" + id);
        payout.setSettlementBatchId(batch.getId()); payout.setSettlementNo(batch.getSettlementNo());
        payout.setProviderCode(batch.getProviderCode()); payout.setCurrencyCode(batch.getCurrencyCode());
        payout.setPayoutAmount(batch.getNetSettlement());
        payout.setSettlementEvidenceJson(batch.getEvidenceSnapshotJson());
        payout.setPayoutPurpose(input.purpose()); payout.setPayeeReference(input.payeeReference());
        payout.setStatus("DRAFT"); payout.setMakerId(operator.id()); payout.setMakerName(operator.name().trim());
        payout.setVersion(0); payout.setCreateTime(now); payout.setUpdateTime(now);
        try {
            payoutMapper.insert(payout);
        } catch (DuplicateKeyException duplicate) {
            throw new ServiceException("payment.settlementPayout.duplicate");
        }

        PaymentSettlementPayoutActionLog action = new PaymentSettlementPayoutActionLog();
        action.setId(IdUtil.getSnowflakeNextId()); action.setTenantId(tenantId); action.setPayoutId(id);
        action.setActionType("CREATE"); action.setAfterStatus("DRAFT"); action.setOperatorId(operator.id());
        action.setOperatorName(operator.name().trim()); action.setEvidenceSnapshotJson(batch.getEvidenceSnapshotJson());
        action.setResultVersion(0); action.setCreateTime(now);
        actionLogMapper.insert(action);
        return detail(payout, List.of(actionVo(action)));
    }

    @Override
    public TableDataInfo<PaymentSettlementPayoutRowVo> queryPage(PaymentSettlementPayoutQueryBo bo, PageQuery pageQuery) {
        PaymentSettlementPayoutQueryBo query = bo == null ? new PaymentSettlementPayoutQueryBo() : bo;
        if (pageQuery == null) throw new ServiceException("payment.settlementPayout.input.invalid");
        if (query.getCreateStart() != null && query.getCreateEnd() != null
            && !query.getCreateStart().before(query.getCreateEnd())) {
            throw new ServiceException("payment.settlementPayout.date.invalid");
        }
        Page<PaymentSettlementPayout> page = payoutMapper.selectPageByTenant(pageQuery.build(), TenantHelper.getTenantId(),
            normalize(query.getPayoutNo()), normalize(query.getSettlementNo()), upper(query.getStatus()),
            upper(query.getProviderCode()), upper(query.getCurrencyCode()), query.getCreateStart(), query.getCreateEnd());
        Page<PaymentSettlementPayoutRowVo> projected = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        projected.setRecords(page.getRecords().stream().map(this::rowVo).toList());
        return TableDataInfo.build(projected);
    }

    @Override
    public PaymentSettlementPayoutDetailVo queryDetail(Long payoutId) {
        String tenantId = TenantHelper.getTenantId();
        PaymentSettlementPayout payout = payoutId == null ? null : payoutMapper.selectByTenantAndId(tenantId, payoutId);
        if (payout == null) throw new ServiceException("payment.settlementPayout.notFound");
        return detail(payout, actionLogMapper.selectByPayout(tenantId, payoutId).stream().map(this::actionVo).toList());
    }

    @Override
    @Transactional
    public PaymentSettlementPayoutDetailVo edit(Long payoutId, PaymentSettlementPayoutEditBo bo) {
        if (bo == null || bo.getVersion() == null || bo.getVersion() < 0) {
            throw new ServiceException("payment.settlementPayout.input.invalid");
        }
        String purpose = text(bo.getPayoutPurpose(), PURPOSE_MAX_LENGTH);
        String reference = text(bo.getPayeeReference(), PAYEE_REFERENCE_MAX_LENGTH);
        String tenantId = TenantHelper.getTenantId();
        PaymentSettlementPayout before = requireCurrent(tenantId, payoutId);
        requireState(before, "DRAFT", "REJECTED");
        requireVersion(before, bo.getVersion());
        PaymentReconciliationOperatorProvider.Operator operator = requireOperator();
        Date now = new Date();
        int updated = payoutMapper.editDraftOrRejected(tenantId, payoutId, bo.getVersion(), purpose, reference, now);
        if (updated != 1) throw classifyFailedUpdate(tenantId, payoutId, "DRAFT", "REJECTED");
        PaymentSettlementPayout after = requireCurrent(tenantId, payoutId);
        insertAction(tenantId, before, after, "EDIT", null, bo.getVersion(), now, operator);
        return detail(after, actionLogMapper.selectByPayout(tenantId, payoutId).stream().map(this::actionVo).toList());
    }

    @Override
    @Transactional
    public PaymentSettlementPayoutDetailVo submit(Long payoutId, PaymentSettlementPayoutCommandBo bo) {
        return transition(payoutId, bo, "DRAFT", "PENDING_APPROVAL", "SUBMIT", commandReason(bo));
    }

    @Override
    @Transactional
    public PaymentSettlementPayoutDetailVo cancel(Long payoutId, PaymentSettlementPayoutCommandBo bo) {
        return transition(payoutId, bo, "DRAFT", "CANCELLED", "CANCEL", commandReason(bo));
    }

    @Override
    @Transactional
    public PaymentSettlementPayoutDetailVo approve(Long payoutId, PaymentSettlementPayoutCommandBo bo) {
        return decision(payoutId, bo, true);
    }

    @Override
    @Transactional
    public PaymentSettlementPayoutDetailVo reject(Long payoutId, PaymentSettlementPayoutCommandBo bo) {
        return decision(payoutId, bo, false);
    }

    private PaymentSettlementPayoutDetailVo decision(Long payoutId, PaymentSettlementPayoutCommandBo bo,
                                                       boolean approve) {
        String tenantId = TenantHelper.getTenantId();
        PaymentSettlementPayout before = requireCurrent(tenantId, payoutId);
        PaymentSettlementPayout after = approve
            ? approvalService.approve(tenantId, before, bo)
            : approvalService.reject(tenantId, before, bo);
        return detail(after, actionLogMapper.selectByPayout(tenantId, payoutId).stream().map(this::actionVo).toList());
    }

    private PaymentSettlementPayoutDetailVo transition(Long payoutId, PaymentSettlementPayoutCommandBo bo,
                                                        String expected, String next, String actionType, String reason) {
        if (bo == null || bo.getVersion() == null || bo.getVersion() < 0) {
            throw new ServiceException("payment.settlementPayout.input.invalid");
        }
        String tenantId = TenantHelper.getTenantId();
        PaymentSettlementPayout before = requireCurrent(tenantId, payoutId);
        requireState(before, expected);
        requireVersion(before, bo.getVersion());
        PaymentReconciliationOperatorProvider.Operator operator = requireOperator();
        Date now = new Date();
        int updated = payoutMapper.transition(tenantId, payoutId, bo.getVersion(), expected, next,
            operator.id(), operator.name(), reason, now);
        if (updated != 1) throw classifyFailedUpdate(tenantId, payoutId, expected);
        PaymentSettlementPayout after = requireCurrent(tenantId, payoutId);
        insertAction(tenantId, before, after, actionType, reason, bo.getVersion(), now, operator);
        return detail(after, actionLogMapper.selectByPayout(tenantId, payoutId).stream().map(this::actionVo).toList());
    }

    private String commandReason(PaymentSettlementPayoutCommandBo bo) {
        if (bo == null) throw new ServiceException("payment.settlementPayout.input.invalid");
        return text(bo.getReason(), REASON_MAX_LENGTH);
    }

    private void insertAction(String tenantId, PaymentSettlementPayout before, PaymentSettlementPayout after,
                              String actionType, String reason, int expectedVersion, Date now,
                              PaymentReconciliationOperatorProvider.Operator operator) {
        PaymentSettlementPayoutActionLog action = new PaymentSettlementPayoutActionLog();
        action.setId(IdUtil.getSnowflakeNextId()); action.setTenantId(tenantId); action.setPayoutId(before.getId());
        action.setActionType(actionType); action.setBeforeStatus(before.getStatus()); action.setAfterStatus(after.getStatus());
        action.setOperatorId(operator.id()); action.setOperatorName(operator.name()); action.setReason(reason);
        action.setEvidenceSnapshotJson(before.getSettlementEvidenceJson()); action.setExpectedVersion(expectedVersion);
        action.setResultVersion(after.getVersion()); action.setCreateTime(now); actionLogMapper.insert(action);
    }

    private PaymentSettlementPayout requireCurrent(String tenantId, Long payoutId) {
        if (payoutId == null || payoutId <= 0) throw new ServiceException("payment.settlementPayout.notFound");
        PaymentSettlementPayout payout = payoutMapper.selectByTenantAndId(tenantId, payoutId);
        if (payout == null) throw new ServiceException("payment.settlementPayout.notFound");
        return payout;
    }

    private void requireState(PaymentSettlementPayout payout, String... allowed) {
        for (String state : allowed) if (state.equals(payout.getStatus())) return;
        throw new ServiceException("payment.settlementPayout.state.invalid");
    }

    private void requireVersion(PaymentSettlementPayout payout, int version) {
        if (payout.getVersion() == null || payout.getVersion() != version) {
            throw new ServiceException("payment.settlementPayout.version.conflict");
        }
    }

    private ServiceException classifyFailedUpdate(String tenantId, Long payoutId, String... allowed) {
        PaymentSettlementPayout current = payoutMapper.selectByTenantAndId(tenantId, payoutId);
        if (current == null) return new ServiceException("payment.settlementPayout.notFound");
        for (String state : allowed) {
            if (state.equals(current.getStatus())) {
                return new ServiceException("payment.settlementPayout.version.conflict");
            }
        }
        return new ServiceException("payment.settlementPayout.state.invalid");
    }

    private PaymentReconciliationOperatorProvider.Operator requireOperator() {
        PaymentReconciliationOperatorProvider.Operator operator = operatorProvider.current();
        if (operator == null || operator.id() == null || blank(operator.name())) {
            throw new ServiceException("payment.settlementPayout.operator.required");
        }
        return new PaymentReconciliationOperatorProvider.Operator(operator.id(), operator.name().trim());
    }

    private CreateInput validateCreate(PaymentSettlementPayoutCreateBo bo) {
        if (bo == null || blank(bo.getSettlementBatchId())) {
            throw new ServiceException("payment.settlementPayout.input.invalid");
        }
        long batchId;
        try {
            batchId = Long.parseLong(bo.getSettlementBatchId().trim());
        } catch (NumberFormatException exception) {
            throw new ServiceException("payment.settlementPayout.input.invalid");
        }
        if (batchId <= 0) throw new ServiceException("payment.settlementPayout.input.invalid");
        String purpose = text(bo.getPayoutPurpose(), PURPOSE_MAX_LENGTH);
        String payeeReference = text(bo.getPayeeReference(), PAYEE_REFERENCE_MAX_LENGTH);
        return new CreateInput(batchId, purpose, payeeReference);
    }

    private String text(String value, int maxLength) {
        if (blank(value)) throw new ServiceException("payment.settlementPayout.input.invalid");
        String normalized = value.trim();
        if (normalized.length() > maxLength || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new ServiceException("payment.settlementPayout.input.invalid");
        }
        return normalized;
    }

    private PaymentSettlementPayoutDetailVo detail(PaymentSettlementPayout payout,
                                                     List<PaymentSettlementPayoutActionLogVo> actions) {
        PaymentSettlementPayoutDetailVo vo = new PaymentSettlementPayoutDetailVo();
        copy(payout, vo); vo.setSettlementEvidenceJson(payout.getSettlementEvidenceJson()); vo.setActionLogs(actions);
        return vo;
    }

    private PaymentSettlementPayoutRowVo rowVo(PaymentSettlementPayout payout) {
        PaymentSettlementPayoutRowVo vo = new PaymentSettlementPayoutRowVo(); copy(payout, vo); return vo;
    }

    private void copy(PaymentSettlementPayout p, PaymentSettlementPayoutRowVo v) {
        v.setId(id(p.getId())); v.setPayoutNo(p.getPayoutNo()); v.setSettlementBatchId(id(p.getSettlementBatchId()));
        v.setSettlementNo(p.getSettlementNo()); v.setProviderCode(p.getProviderCode()); v.setCurrencyCode(p.getCurrencyCode());
        v.setPayoutAmount(decimal(p.getPayoutAmount())); v.setPayoutPurpose(p.getPayoutPurpose());
        v.setPayeeReference(p.getPayeeReference()); v.setStatus(p.getStatus()); v.setMakerId(id(p.getMakerId()));
        v.setMakerName(p.getMakerName()); v.setSubmitterId(id(p.getSubmitterId())); v.setSubmitterName(p.getSubmitterName());
        v.setReviewerId(id(p.getReviewerId())); v.setReviewerName(p.getReviewerName());
        v.setDecisionReason(p.getDecisionReason()); v.setVersion(p.getVersion()); v.setSubmittedTime(p.getSubmittedTime());
        v.setReviewedTime(p.getReviewedTime()); v.setCreateTime(p.getCreateTime()); v.setUpdateTime(p.getUpdateTime());
    }

    private PaymentSettlementPayoutActionLogVo actionVo(PaymentSettlementPayoutActionLog a) {
        PaymentSettlementPayoutActionLogVo v = new PaymentSettlementPayoutActionLogVo();
        v.setId(id(a.getId())); v.setPayoutId(id(a.getPayoutId())); v.setActionType(a.getActionType());
        v.setBeforeStatus(a.getBeforeStatus()); v.setAfterStatus(a.getAfterStatus());
        v.setOperatorId(id(a.getOperatorId())); v.setOperatorName(a.getOperatorName()); v.setReason(a.getReason());
        v.setEvidenceSnapshotJson(a.getEvidenceSnapshotJson()); v.setExpectedVersion(a.getExpectedVersion());
        v.setResultVersion(a.getResultVersion()); v.setCreateTime(a.getCreateTime()); return v;
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String normalize(String value) { return blank(value) ? null : value.trim(); }
    private static String upper(String value) {
        String normalized = normalize(value); return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
    private static String id(Long value) { return value == null ? null : value.toString(); }
    private static String decimal(BigDecimal value) { return value == null ? null : value.setScale(6).toPlainString(); }

    private record CreateInput(long batchId, String purpose, String payeeReference) { }
}
