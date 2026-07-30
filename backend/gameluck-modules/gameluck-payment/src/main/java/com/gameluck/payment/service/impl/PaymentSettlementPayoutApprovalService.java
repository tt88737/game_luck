package com.gameluck.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.payment.domain.PaymentSettlementPayout;
import com.gameluck.payment.domain.PaymentSettlementPayoutActionLog;
import com.gameluck.payment.domain.bo.PaymentSettlementPayoutCommandBo;
import com.gameluck.payment.mapper.PaymentSettlementPayoutActionLogMapper;
import com.gameluck.payment.mapper.PaymentSettlementPayoutMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentSettlementPayoutApprovalService {
    private static final int REASON_MAX_LENGTH = 500;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> SENSITIVE_KEYS = Set.of(
        "bank", "bankaccount", "account", "accountnumber", "routingnumber", "credential",
        "secret", "token", "signature", "rawbody");

    private final PaymentSettlementPayoutMapper payoutMapper;
    private final PaymentSettlementPayoutActionLogMapper actionLogMapper;
    private final PaymentReconciliationOperatorProvider operatorProvider;

    @Transactional
    public PaymentSettlementPayout approve(String tenantId, PaymentSettlementPayout payout,
                                            PaymentSettlementPayoutCommandBo bo) {
        return decide(tenantId, payout, bo, "APPROVED", "APPROVE");
    }

    @Transactional
    public PaymentSettlementPayout reject(String tenantId, PaymentSettlementPayout payout,
                                           PaymentSettlementPayoutCommandBo bo) {
        return decide(tenantId, payout, bo, "REJECTED", "REJECT");
    }

    private PaymentSettlementPayout decide(String tenantId, PaymentSettlementPayout payout,
                                            PaymentSettlementPayoutCommandBo bo, String next, String actionType) {
        if (payout == null) throw new ServiceException("payment.settlementPayout.notFound");
        if (!tenantId.equals(payout.getTenantId()) || !"PENDING_APPROVAL".equals(payout.getStatus())) {
            throw new ServiceException("payment.settlementPayout.state.invalid");
        }
        if (bo == null || bo.getVersion() == null || bo.getVersion() < 0 || bo.getReason() == null
            || bo.getReason().isBlank()) throw new ServiceException("payment.settlementPayout.input.invalid");
        String reason = bo.getReason().trim();
        if (reason.length() > REASON_MAX_LENGTH || reason.chars().anyMatch(Character::isISOControl)) {
            throw new ServiceException("payment.settlementPayout.input.invalid");
        }
        if (payout.getVersion() == null || !payout.getVersion().equals(bo.getVersion())) {
            throw new ServiceException("payment.settlementPayout.version.conflict");
        }
        PaymentReconciliationOperatorProvider.Operator operator = operatorProvider.current();
        if (operator == null || operator.id() == null || operator.name() == null || operator.name().isBlank()) {
            throw new ServiceException("payment.settlementPayout.operator.required");
        }
        if (operator.id().equals(payout.getMakerId())) {
            throw new ServiceException("payment.settlementPayout.selfApproval");
        }
        String operatorName = operator.name().trim();
        Date now = new Date();
        int updated = payoutMapper.transition(tenantId, payout.getId(), bo.getVersion(), "PENDING_APPROVAL", next,
            operator.id(), operatorName, reason, now);
        if (updated != 1) throw classifyFailedUpdate(tenantId, payout.getId());
        PaymentSettlementPayout after = payoutMapper.selectByTenantAndId(tenantId, payout.getId());
        if (after == null) throw new ServiceException("payment.settlementPayout.notFound");

        PaymentSettlementPayoutActionLog action = new PaymentSettlementPayoutActionLog();
        action.setId(IdUtil.getSnowflakeNextId()); action.setTenantId(tenantId); action.setPayoutId(payout.getId());
        action.setActionType(actionType); action.setBeforeStatus("PENDING_APPROVAL"); action.setAfterStatus(next);
        action.setOperatorId(operator.id()); action.setOperatorName(operatorName); action.setReason(reason);
        action.setEvidenceSnapshotJson(sanitizeEvidence(payout.getSettlementEvidenceJson()));
        action.setExpectedVersion(bo.getVersion());
        action.setResultVersion(after.getVersion()); action.setCreateTime(now); actionLogMapper.insert(action);
        return after;
    }

    private ServiceException classifyFailedUpdate(String tenantId, Long payoutId) {
        PaymentSettlementPayout current = payoutMapper.selectByTenantAndId(tenantId, payoutId);
        if (current == null) return new ServiceException("payment.settlementPayout.notFound");
        if ("PENDING_APPROVAL".equals(current.getStatus())) {
            return new ServiceException("payment.settlementPayout.version.conflict");
        }
        return new ServiceException("payment.settlementPayout.state.invalid");
    }

    private String sanitizeEvidence(String evidence) {
        if (evidence == null || evidence.isBlank()) return null;
        try {
            JsonNode root = JSON.readTree(evidence);
            removeSensitiveFields(root);
            return JSON.writeValueAsString(root);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void removeSensitiveFields(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String normalized = field.getKey().replaceAll("[^A-Za-z0-9]", "").toLowerCase();
                if (SENSITIVE_KEYS.contains(normalized)) fields.remove();
                else removeSensitiveFields(field.getValue());
            }
        } else if (node.isArray()) {
            node.forEach(this::removeSensitiveFields);
        }
    }
}
