package com.gameluck.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.domain.PaymentReconciliationBatch;
import com.gameluck.payment.domain.PaymentReconciliationLine;
import com.gameluck.payment.domain.PaymentReconciliationIssue;
import com.gameluck.payment.domain.PaymentReconciliationActionLog;
import com.gameluck.payment.domain.bo.PaymentReconciliationBatchBo;
import com.gameluck.payment.domain.bo.PaymentReconciliationIssueBo;
import com.gameluck.payment.domain.bo.PaymentReconciliationResolutionBo;
import com.gameluck.payment.domain.vo.PaymentReconciliationBatchDetailVo;
import com.gameluck.payment.domain.vo.PaymentReconciliationBatchVo;
import com.gameluck.payment.domain.vo.PaymentReconciliationLineVo;
import com.gameluck.payment.domain.vo.PaymentReconciliationIssueVo;
import com.gameluck.payment.domain.vo.PaymentReconciliationIssueDetailVo;
import com.gameluck.payment.domain.vo.PaymentReconciliationActionLogVo;
import com.gameluck.payment.mapper.PaymentReconciliationBatchMapper;
import com.gameluck.payment.mapper.PaymentReconciliationLineMapper;
import com.gameluck.payment.mapper.PaymentReconciliationIssueMapper;
import com.gameluck.payment.mapper.PaymentReconciliationActionLogMapper;
import com.gameluck.payment.provider.PaymentProviderRegistry;
import com.gameluck.payment.service.IPaymentReconciliationService;
import com.gameluck.payment.service.reconciliation.PaymentReconciliationCsvParser;
import com.gameluck.payment.service.reconciliation.ReconciliationParseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentReconciliationServiceImpl implements IPaymentReconciliationService {
    private final PaymentReconciliationBatchMapper batchMapper;
    private final PaymentReconciliationLineMapper lineMapper;
    private final PaymentProviderRegistry providerRegistry;
    private final PaymentReconciliationBatchCreator batchCreator;
    private final PaymentReconciliationValidationWorker validationWorker;
    private final PaymentReconciliationFailureRecorder failureRecorder;
    private final PaymentReconciliationUploadSpooler uploadSpooler;
    private final PaymentReconciliationOperatorProvider operatorProvider;
    private PaymentReconciliationIssueMapper issueMapper;
    private PaymentReconciliationActionLogMapper actionLogMapper;
    private PaymentReconciliationResolutionService resolutionService;

    @org.springframework.beans.factory.annotation.Autowired
    void setIssueSupport(PaymentReconciliationIssueMapper issueMapper,
                         PaymentReconciliationActionLogMapper actionLogMapper,
                         PaymentReconciliationResolutionService resolutionService) {
        this.issueMapper = issueMapper;
        this.actionLogMapper = actionLogMapper;
        this.resolutionService = resolutionService;
    }
    private PaymentReconciliationExecutionService executionService;

    @org.springframework.beans.factory.annotation.Autowired
    void setExecutionService(PaymentReconciliationExecutionService executionService) {
        this.executionService = executionService;
    }

    @Override
    public PaymentReconciliationBatchDetailVo execute(Long batchId) {
        if (executionService == null) throw new ServiceException("Reconciliation execution is unavailable");
        return executionService.execute(batchId);
    }

    @Override
    public PaymentReconciliationBatchDetailVo upload(String providerCode, LocalDate statementDate,
                                                       String originalFileName, long size, InputStream input) {
        if (statementDate == null || input == null) throw new ServiceException(MessageUtils.message("payment.reconciliation.upload.input.required"));
        String safeFileName = sanitize(originalFileName);
        String tenantId = TenantHelper.getTenantId();
        String normalizedProvider = providerRegistry.resolve(providerCode).providerCode().trim().toUpperCase(Locale.ROOT);
        try (PaymentReconciliationUploadSpooler.Spool spool = uploadSpooler.spool(input, size)) {
            if (batchMapper.selectByDigest(tenantId, normalizedProvider, spool.digest()) != null) {
                throw duplicateError();
            }
            PaymentReconciliationBatch batch = new PaymentReconciliationBatch();
            PaymentReconciliationOperatorProvider.Operator operator = operatorProvider.current();
            if (operator.id() == null) throw new ServiceException(MessageUtils.message("payment.reconciliation.operator.required"));
            String operatorName = operator.name() == null ? "" : operator.name().trim();
            if (operatorName.isEmpty()) throw new ServiceException(MessageUtils.message("payment.reconciliation.operator.name.required"));
            batch.setId(IdUtil.getSnowflakeNextId()); batch.setTenantId(tenantId); batch.setProviderCode(normalizedProvider);
            batch.setStatementDate(java.sql.Date.valueOf(statementDate)); batch.setOriginalFileName(safeFileName);
            batch.setFileDigest(spool.digest()); batch.setStatus("UPLOADED"); batch.setVersion(0); batch.setCreateTime(new Date());
            batch.setCreatorId(operator.id()); batch.setCreatorName(operatorName);
            try {
                batchCreator.create(batch);
            } catch (DataIntegrityViolationException e) {
                if (isDigestDuplicate(e)) throw duplicateError();
                throw new ServiceException(MessageUtils.message("payment.reconciliation.upload.failed"));
            }
            PaymentReconciliationValidationWorker.ValidationResult result;
            try {
                result = validationWorker.validate(tenantId, batch, spool.path(), spool.size(),
                    new PaymentReconciliationValidationWorker.UploadAction(operator.id(), operatorName));
            } catch (PaymentReconciliationFileException e) {
                throw new ServiceException(fileErrorMessage(e.code()));
            } catch (PaymentReconciliationValidationWorker.UploadActionPersistenceException e) {
                throw new ServiceException(MessageUtils.message("payment.reconciliation.upload.failed"));
            } catch (ServiceException e) {
                throw e;
            } catch (RuntimeException e) {
                failureRecorder.record(tenantId, batch.getId(), "RECONCILIATION_FILE_PROCESSING_FAILED");
                throw new ServiceException(MessageUtils.message("payment.reconciliation.upload.processing.failed"));
            }
            batch.setTotalCount(result.total()); batch.setValidCount(result.valid()); batch.setInvalidCount(result.invalid());
            batch.setMatchedCount(0); batch.setDiscrepancyCount(0); batch.setStatus("VALIDATED"); batch.setUpdateTime(result.completedAt());
            return detail(batch);
        }
    }

    @Override
    public TableDataInfo<PaymentReconciliationBatchVo> queryPage(PaymentReconciliationBatchBo bo, PageQuery pageQuery) {
        PaymentReconciliationBatchBo query = bo == null ? new PaymentReconciliationBatchBo() : bo;
        Page<PaymentReconciliationBatch> page = batchMapper.selectPageByTenant(pageQuery.build(), TenantHelper.getTenantId(), query);
        Page<PaymentReconciliationBatchVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::batchVo).toList());
        return TableDataInfo.build(result);
    }

    @Override
    public PaymentReconciliationBatchDetailVo queryDetail(Long batchId) {
        PaymentReconciliationBatch batch = batchMapper.selectByTenantAndId(TenantHelper.getTenantId(), batchId);
        if (batch == null) throw new ServiceException(MessageUtils.message("payment.reconciliation.batch.notFound"));
        return detail(batch);
    }

    @Override
    public TableDataInfo<PaymentReconciliationLineVo> queryLines(Long batchId, String lineStatus, PageQuery pageQuery) {
        String tenantId = TenantHelper.getTenantId();
        if (batchMapper.selectByTenantAndId(tenantId, batchId) == null) throw new ServiceException(MessageUtils.message("payment.reconciliation.batch.notFound"));
        Page<PaymentReconciliationLine> page = lineMapper.selectPageByBatch(pageQuery.build(), tenantId, batchId, lineStatus);
        Page<PaymentReconciliationLineVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::lineVo).toList());
        return TableDataInfo.build(result);
    }

    @Override
    public TableDataInfo<PaymentReconciliationIssueVo> queryIssues(Long batchId, PaymentReconciliationIssueBo bo, PageQuery pageQuery) {
        String tenantId = TenantHelper.getTenantId();
        if (batchMapper.selectByTenantAndId(tenantId, batchId) == null) throw new ServiceException(MessageUtils.message("payment.reconciliation.batch.notFound"));
        PaymentReconciliationIssueBo query = bo == null ? new PaymentReconciliationIssueBo() : bo;
        Page<PaymentReconciliationIssue> page = issueMapper.selectPageByBatchFiltered(pageQuery.build(), tenantId, batchId, query);
        Page<PaymentReconciliationIssueVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::issueVo).toList());
        return TableDataInfo.build(result);
    }

    @Override
    public PaymentReconciliationIssueDetailVo queryIssueDetail(Long issueId) {
        String tenantId = TenantHelper.getTenantId();
        PaymentReconciliationIssue issue = issueMapper.selectByTenantAndId(tenantId, issueId);
        if (issue == null) throw new ServiceException(MessageUtils.message("payment.reconciliation.issue.notFound"));
        PaymentReconciliationIssueDetailVo vo = issueDetailVo(issue);
        vo.setActionLogs(actionLogMapper.selectByIssue(tenantId, issueId).stream().map(this::actionLogVo).toList());
        if (issue.getLineId() == null) {
            vo.setPlatformOnly(true);
        } else {
            PaymentReconciliationLine sourceLine = lineMapper.selectByTenantAndId(tenantId, issue.getLineId());
            if (sourceLine != null) {
                vo.setSourceRowNumber(sourceLine.getSourceRowNumber());
                vo.setSourceLine(lineVo(sourceLine));
                vo.setCanonicalOriginalFields(sourceLine.getRawFieldsJson());
            }
        }
        return vo;
    }

    @Override
    public PaymentReconciliationIssueDetailVo resolve(Long issueId, PaymentReconciliationResolutionBo bo) {
        resolutionService.resolve(issueId, bo);
        return queryIssueDetail(issueId);
    }

    @Override
    public PaymentReconciliationIssueDetailVo ignore(Long issueId, PaymentReconciliationResolutionBo bo) {
        resolutionService.ignore(issueId, bo);
        return queryIssueDetail(issueId);
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) return "statement.csv";
        String normalized = name.replace('\\', '/');
        String result = normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        String safe = result.isEmpty() ? "statement.csv" : result;
        if (safe.codePointCount(0, safe.length()) > 255) {
            throw new ServiceException(MessageUtils.message("payment.reconciliation.upload.filename.tooLong"));
        }
        return safe;
    }

    private String fileErrorMessage(String code) {
        return switch (code) {
            case "FILE_TOO_LARGE" -> MessageUtils.message("payment.reconciliation.upload.file.tooLarge");
            case "INVALID_UTF8" -> MessageUtils.message("payment.reconciliation.upload.file.invalidUtf8");
            case "INVALID_HEADER" -> MessageUtils.message("payment.reconciliation.upload.file.invalidHeader");
            case "ROW_LIMIT_EXCEEDED" -> MessageUtils.message("payment.reconciliation.upload.file.rowLimitExceeded");
            default -> MessageUtils.message("payment.reconciliation.upload.file.readFailed");
        };
    }

    private ServiceException duplicateError() {
        return new ServiceException(MessageUtils.message("payment.reconciliation.upload.duplicate"));
    }

    private boolean isDigestDuplicate(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT)
                .contains("uk_gl_payment_reconciliation_batch_01")) return true;
        }
        return false;
    }

    private PaymentReconciliationBatchVo batchVo(PaymentReconciliationBatch b) {
        PaymentReconciliationBatchVo v = new PaymentReconciliationBatchVo(); copyBatch(b, v); return v;
    }
    private PaymentReconciliationBatchDetailVo detail(PaymentReconciliationBatch b) {
        PaymentReconciliationBatchDetailVo v = new PaymentReconciliationBatchDetailVo(); copyBatch(b, v); return v;
    }
    private void copyBatch(PaymentReconciliationBatch b, PaymentReconciliationBatchVo v) {
        v.setId(id(b.getId())); v.setTenantId(b.getTenantId()); v.setProviderCode(b.getProviderCode()); v.setStatementDate(b.getStatementDate());
        v.setOriginalFileName(b.getOriginalFileName()); v.setFileDigest(b.getFileDigest()); v.setTotalCount(b.getTotalCount());
        v.setValidCount(b.getValidCount()); v.setInvalidCount(b.getInvalidCount()); v.setMatchedCount(b.getMatchedCount());
        v.setDiscrepancyCount(b.getDiscrepancyCount()); v.setStatus(b.getStatus()); v.setFailureReason(b.getFailureReason());
        v.setCreatorId(id(b.getCreatorId())); v.setCreatorName(b.getCreatorName()); v.setCreateTime(b.getCreateTime()); v.setUpdateTime(b.getUpdateTime());
    }
    private PaymentReconciliationLineVo lineVo(PaymentReconciliationLine l) {
        PaymentReconciliationLineVo v = new PaymentReconciliationLineVo(); v.setId(id(l.getId())); v.setBatchId(id(l.getBatchId()));
        v.setSourceRowNumber(l.getSourceRowNumber()); v.setProviderRecordId(l.getProviderRecordId()); v.setEventType(l.getEventType());
        v.setProviderSessionNo(l.getProviderSessionNo()); v.setPurchaseOrderNo(l.getPurchaseOrderNo()); v.setCurrencyCode(l.getCurrencyCode());
        v.setAmount(money(l.getAmount())); v.setOccurredTime(l.getOccurredTime()); v.setStatus(l.getStatus()); v.setParseError(l.getParseError());
        v.setRawFieldsJson(l.getRawFieldsJson()); v.setCreateTime(l.getCreateTime()); return v;
    }

    private PaymentReconciliationIssueVo issueVo(PaymentReconciliationIssue i) {
        PaymentReconciliationIssueVo v = new PaymentReconciliationIssueVo(); copyIssue(i, v); return v;
    }
    private PaymentReconciliationIssueDetailVo issueDetailVo(PaymentReconciliationIssue i) {
        PaymentReconciliationIssueDetailVo v = new PaymentReconciliationIssueDetailVo(); copyIssue(i, v); return v;
    }
    private void copyIssue(PaymentReconciliationIssue i, PaymentReconciliationIssueVo v) {
        v.setId(id(i.getId())); v.setBatchId(id(i.getBatchId())); v.setLineId(id(i.getLineId())); v.setIssueType(i.getIssueType()); v.setStatus(i.getStatus());
        v.setPaymentSessionId(id(i.getPaymentSessionId())); v.setSessionNo(i.getSessionNo()); v.setPurchaseOrderId(id(i.getPurchaseOrderId())); v.setPurchaseOrderNo(i.getPurchaseOrderNo());
        v.setWebhookEventId(id(i.getWebhookEventId())); v.setReversalId(id(i.getReversalId())); v.setProviderEventType(i.getProviderEventType()); v.setPlatformEventType(i.getPlatformEventType());
        v.setProviderCurrencyCode(i.getProviderCurrencyCode()); v.setPlatformCurrencyCode(i.getPlatformCurrencyCode()); v.setProviderAmount(money(i.getProviderAmount())); v.setPlatformAmount(money(i.getPlatformAmount()));
        v.setProviderStatus(i.getProviderStatus()); v.setPlatformStatus(i.getPlatformStatus()); v.setDiagnosticSnapshotJson(i.getDiagnosticSnapshotJson()); v.setResolutionType(i.getResolutionType());
        v.setResolutionRemark(i.getResolutionRemark()); v.setResolvedBy(id(i.getResolvedBy())); v.setResolvedTime(i.getResolvedTime()); v.setVersion(i.getVersion()); v.setCreateTime(i.getCreateTime()); v.setUpdateTime(i.getUpdateTime());
    }
    private PaymentReconciliationActionLogVo actionLogVo(PaymentReconciliationActionLog l) {
        PaymentReconciliationActionLogVo v = new PaymentReconciliationActionLogVo(); v.setId(id(l.getId())); v.setBatchId(id(l.getBatchId())); v.setIssueId(id(l.getIssueId()));
        v.setActionType(l.getActionType()); v.setBeforeStatus(l.getBeforeStatus()); v.setAfterStatus(l.getAfterStatus()); v.setOperatorId(id(l.getOperatorId()));
        v.setOperatorName(l.getOperatorName()); v.setRemark(l.getRemark()); v.setCreateTime(l.getCreateTime()); return v;
    }
    private String id(Long value) { return value == null ? null : value.toString(); }
    private String money(java.math.BigDecimal value) { return value == null ? null : value.toPlainString(); }
}
