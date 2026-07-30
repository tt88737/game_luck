package com.gameluck.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.payment.domain.PaymentReconciliationBatch;
import com.gameluck.payment.domain.PaymentReconciliationLine;
import com.gameluck.payment.domain.PaymentReconciliationActionLog;
import com.gameluck.payment.mapper.PaymentReconciliationBatchMapper;
import com.gameluck.payment.mapper.PaymentReconciliationLineMapper;
import com.gameluck.payment.mapper.PaymentReconciliationActionLogMapper;
import com.gameluck.payment.service.reconciliation.ReconciliationParseResult;
import com.gameluck.payment.service.reconciliation.ReconciliationParsedLine;
import com.gameluck.payment.service.reconciliation.PaymentReconciliationCsvParser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.InputStream;
import java.util.HexFormat;
import java.security.MessageDigest;

@Component
public class PaymentReconciliationValidationWorker {
    static final int INSERT_CHUNK_SIZE = 500;
    private final PaymentReconciliationLineMapper lineMapper;
    private final PaymentReconciliationBatchMapper batchMapper;
    private final PaymentReconciliationActionLogMapper actionLogMapper;
    private final PaymentReconciliationCsvParser csvParser;

    @org.springframework.beans.factory.annotation.Autowired
    public PaymentReconciliationValidationWorker(PaymentReconciliationLineMapper lineMapper,
            PaymentReconciliationBatchMapper batchMapper, PaymentReconciliationActionLogMapper actionLogMapper,
            PaymentReconciliationCsvParser csvParser) {
        this.lineMapper = lineMapper; this.batchMapper = batchMapper;
        this.actionLogMapper = actionLogMapper; this.csvParser = csvParser;
    }

    PaymentReconciliationValidationWorker(PaymentReconciliationLineMapper lineMapper,
            PaymentReconciliationBatchMapper batchMapper, PaymentReconciliationCsvParser csvParser) {
        this(lineMapper, batchMapper, null, csvParser);
    }

    @Transactional
    public ValidationResult validate(String tenantId, PaymentReconciliationBatch batch, Path path, long size) {
        return validate(tenantId, batch, path, size, null);
    }

    @Transactional
    public ValidationResult validate(String tenantId, PaymentReconciliationBatch batch, Path path, long size,
                                     UploadAction action) {
        ReconciliationParseResult parsed;
        try (InputStream input = Files.newInputStream(path)) {
            parsed = csvParser.parse(input, size);
        } catch (PaymentReconciliationFileException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read reconciliation spool", e);
        }
        verifyDigest(parsed.sha256Digest(), batch.getFileDigest());
        if (parsed.fileErrorCode() != null) throw new PaymentReconciliationFileException(parsed.fileErrorCode());
        return persist(tenantId, batch, parsed, action);
    }

    private void verifyDigest(String actual, String expected) {
        try {
            byte[] actualBytes = HexFormat.of().parseHex(actual);
            byte[] expectedBytes = HexFormat.of().parseHex(expected);
            if (!MessageDigest.isEqual(expectedBytes, actualBytes)) throw new SecurityException("Reconciliation spool integrity check failed");
        } catch (SecurityException e) { throw e; }
        catch (RuntimeException e) { throw new IllegalStateException("Invalid reconciliation digest", e); }
    }

    ValidationResult persist(String tenantId, PaymentReconciliationBatch batch, ReconciliationParseResult parsed) {
        return persist(tenantId, batch, parsed, null);
    }

    ValidationResult persist(String tenantId, PaymentReconciliationBatch batch, ReconciliationParseResult parsed,
                             UploadAction action) {
        List<PaymentReconciliationLine> chunk = new ArrayList<>(INSERT_CHUNK_SIZE);
        for (ReconciliationParsedLine source : parsed.lines()) {
            chunk.add(toEntity(tenantId, batch.getId(), source));
            if (chunk.size() == INSERT_CHUNK_SIZE) {
                lineMapper.insertBatch(List.copyOf(chunk));
                chunk.clear();
            }
        }
        if (!chunk.isEmpty()) lineMapper.insertBatch(List.copyOf(chunk));
        int total = Math.toIntExact(parsed.totalCount());
        int valid = Math.toIntExact(parsed.validCount());
        int invalid = Math.toIntExact(parsed.invalidCount());
        Date now = new Date();
        if (batchMapper.finalizeValidation(tenantId, batch.getId(), total, valid, invalid, now) != 1) {
            throw new ServiceException(MessageUtils.message("payment.reconciliation.execute.stateConflict"));
        }
        if (action != null && actionLogMapper != null) {
            PaymentReconciliationActionLog log = new PaymentReconciliationActionLog();
            log.setId(IdUtil.getSnowflakeNextId()); log.setTenantId(tenantId); log.setBatchId(batch.getId());
            log.setActionType("UPLOAD"); log.setBeforeStatus("UPLOADED"); log.setAfterStatus("VALIDATED");
            log.setOperatorId(action.operatorId()); log.setOperatorName(action.operatorName()); log.setCreateTime(now);
            try {
                actionLogMapper.insert(log);
            } catch (RuntimeException exception) {
                throw new UploadActionPersistenceException(exception);
            }
        }
        return new ValidationResult(total, valid, invalid, now);
    }

    private PaymentReconciliationLine toEntity(String tenantId, Long batchId, ReconciliationParsedLine source) {
        PaymentReconciliationLine line = new PaymentReconciliationLine();
        line.setId(IdUtil.getSnowflakeNextId()); line.setTenantId(tenantId); line.setBatchId(batchId);
        line.setSourceRowNumber(source.sourceLineNumber()); line.setProviderRecordId(source.providerRecordId());
        line.setEventType(source.eventType()); line.setProviderSessionNo(source.providerSessionNo());
        line.setPurchaseOrderNo(source.purchaseOrderNo()); line.setCurrencyCode(source.currency());
        line.setAmount(source.amount()); line.setOccurredTime(source.occurredTime() == null ? null : Date.from(source.occurredTime()));
        line.setStatus(source.status().name()); line.setParseError(source.parseErrorCode());
        line.setRawFieldsJson(source.sourceFieldsJson()); line.setCreateTime(new Date());
        return line;
    }

    public record ValidationResult(int total, int valid, int invalid, Date completedAt) { }
    public record UploadAction(Long operatorId, String operatorName) { }

    public static final class UploadActionPersistenceException extends RuntimeException {
        UploadActionPersistenceException(Throwable cause) { super("Unable to persist reconciliation upload action", cause); }
    }
}
