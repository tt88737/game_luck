package com.gameluck.payment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.mybatis.helper.MemberNoQueryHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.common.satoken.utils.LoginHelper;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.domain.vo.MemberProfileVo;
import com.gameluck.member.mapper.MemberProfileMapper;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.domain.PurchaseReversal;
import com.gameluck.payment.domain.PurchaseReversalItem;
import com.gameluck.payment.domain.PurchaseReversalReviewLog;
import com.gameluck.payment.domain.bo.PurchaseReversalReviewActionBo;
import com.gameluck.payment.domain.bo.PurchaseReversalReviewBo;
import com.gameluck.payment.domain.vo.PurchaseOrderGrantSnapshotVo;
import com.gameluck.payment.domain.vo.PurchaseOrderVo;
import com.gameluck.payment.domain.vo.PurchasePaymentEventVo;
import com.gameluck.payment.domain.vo.PurchaseReversalItemVo;
import com.gameluck.payment.domain.vo.PurchaseReversalReviewDetailVo;
import com.gameluck.payment.domain.vo.PurchaseReversalReviewLogVo;
import com.gameluck.payment.domain.vo.PurchaseReversalReviewVo;
import com.gameluck.payment.domain.vo.PurchaseReversalReviewActionResultVo;
import com.gameluck.payment.enums.PurchaseReversalDispositionStatus;
import com.gameluck.payment.enums.PurchaseOrderStatus;
import com.gameluck.payment.enums.PurchaseReversalReviewOperationType;
import com.gameluck.payment.enums.PurchaseReversalStatus;
import com.gameluck.payment.enums.PurchaseReversalType;
import com.gameluck.payment.mapper.PurchaseOrderGrantSnapshotMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.mapper.PurchasePaymentEventMapper;
import com.gameluck.payment.mapper.PurchaseReversalItemMapper;
import com.gameluck.payment.mapper.PurchaseReversalMapper;
import com.gameluck.payment.mapper.PurchaseReversalReviewLogMapper;
import com.gameluck.payment.service.IPurchaseReversalReviewService;
import com.gameluck.wallet.domain.bo.WalletBatchDebitBo;
import com.gameluck.wallet.domain.bo.WalletBatchDebitLineBo;
import com.gameluck.wallet.domain.vo.WalletBatchDebitLineResult;
import com.gameluck.wallet.domain.vo.WalletBatchDebitPreviewLineResult;
import com.gameluck.wallet.domain.vo.WalletBatchDebitPreviewResult;
import com.gameluck.wallet.domain.vo.WalletBatchDebitResult;
import com.gameluck.wallet.service.IWalletCoreService;
import com.gameluck.wallet.service.IWalletTurnoverTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class PurchaseReversalReviewServiceImpl implements IPurchaseReversalReviewService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(6);

    private final PurchaseReversalMapper reversalMapper;
    private final PurchaseReversalItemMapper itemMapper;
    private final PurchaseReversalReviewLogMapper logMapper;
    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderGrantSnapshotMapper snapshotMapper;
    private final PurchasePaymentEventMapper eventMapper;
    private final MemberProfileMapper memberMapper;
    private final JdbcTemplate jdbcTemplate;
    private final IWalletCoreService walletCoreService;
    private final IWalletTurnoverTaskService turnoverTaskService;
    private final ObjectMapper objectMapper;

    @Override
    public TableDataInfo<PurchaseReversalReviewVo> queryPageList(PurchaseReversalReviewBo bo, PageQuery pageQuery) {
        String tenantId = TenantHelper.getTenantId();
        Page<PurchaseReversal> page = reversalMapper.selectPage(pageQuery.build(), buildQuery(tenantId, bo));
        List<PurchaseReversalReviewVo> rows = page.getRecords().stream().map(this::toListVo).toList();
        MemberNoQueryHelper.fillMemberNo(jdbcTemplate, rows, PurchaseReversalReviewVo::getMemberId,
            PurchaseReversalReviewVo::setMemberNo);
        return new TableDataInfo<>(rows, page.getTotal());
    }

    @Override
    public PurchaseReversalReviewDetailVo queryByReversalNo(String reversalNo) {
        String tenantId = TenantHelper.getTenantId();
        PurchaseReversal reversal = reversalMapper.selectByReversalNo(tenantId, reversalNo);
        if (reversal == null) {
            throw new ServiceException(MessageUtils.message("payment.purchase.reversal.review.not.exists"));
        }
        PurchaseReversalReviewDetailVo detail = BeanUtil.toBean(reversal, PurchaseReversalReviewDetailVo.class);
        detail.setItems(items(tenantId, reversalNo));
        PurchaseOrder order = orderMapper.selectOne(Wrappers.<PurchaseOrder>lambdaQuery()
            .eq(PurchaseOrder::getTenantId, tenantId)
            .eq(PurchaseOrder::getPurchaseOrderNo, reversal.getPurchaseOrderNo())
            .last("limit 1"));
        if (order != null) {
            detail.setPurchaseOrder(BeanUtil.toBean(order, PurchaseOrderVo.class));
            detail.setGrantSnapshots(snapshotMapper.selectByPurchaseOrderNo(tenantId, order.getPurchaseOrderNo()).stream()
                .map(row -> BeanUtil.toBean(row, PurchaseOrderGrantSnapshotVo.class)).toList());
            detail.setPaymentEvents(eventMapper.selectByPurchaseOrderNo(tenantId, order.getPurchaseOrderNo()).stream()
                .map(row -> BeanUtil.toBean(row, PurchasePaymentEventVo.class)).toList());
        }
        MemberProfile member = memberMapper.selectOne(Wrappers.<MemberProfile>lambdaQuery()
            .eq(MemberProfile::getTenantId, tenantId).eq(MemberProfile::getId, reversal.getMemberId()).last("limit 1"));
        if (member != null) {
            detail.setMember(BeanUtil.toBean(member, MemberProfileVo.class));
            detail.setRiskLevel(member.getRiskLevel());
        }
        detail.setReviewLogs(logMapper.selectByReversalNo(tenantId, reversalNo).stream()
            .map(row -> BeanUtil.toBean(row, PurchaseReversalReviewLogVo.class)).toList());
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReversalReviewActionResultVo retry(String reversalNo, PurchaseReversalReviewActionBo bo) {
        String tenantId = TenantHelper.getTenantId();
        String requestKey = requireRequestKey(bo);
        PurchaseReversalReviewLog replay = logMapper.selectByRequestKey(tenantId, requestKey);
        if (replay != null) {
            if (!reversalNo.equals(replay.getReversalNo()) || PurchaseReversalReviewOperationType.LOSS_ACCEPTED.name().equals(replay.getOperationType())) {
                throw new ServiceException(MessageUtils.message("payment.purchase.reversal.review.request.conflict"));
            }
            return actionResult(reversalNo, replay.getOperationType());
        }
        PurchaseReversal reversal = lockPending(tenantId, reversalNo);
        PurchaseOrder order = lockReviewOrder(tenantId, reversal);
        List<PurchaseReversalItem> items = requireItems(tenantId, reversalNo);
        Date now = new Date();
        WalletBatchDebitResult wallet = walletCoreService.batchDebit(walletRequest(reversal, items));
        Map<String, WalletBatchDebitLineResult> lines = new LinkedHashMap<>();
        wallet.getLines().forEach(line -> lines.put(line.getCurrencyCode(), line));
        boolean completed = PurchaseReversalStatus.COMPLETED.name().equals(wallet.getStatus());
        for (PurchaseReversalItem item : items) {
            WalletBatchDebitLineResult line = lines.get(item.getCurrencyCode());
            requireWalletLine(line);
            item.setAvailableAmount(scale(line.getAvailableAmount()));
            item.setShortfallAmount(scale(line.getShortfallAmount()));
            if (completed) {
                item.setRecoveredAmount(scale(line.getRecoveredAmount()));
                item.setWalletTransactionNo(line.getWalletTransactionNo());
                item.setStatus(PurchaseReversalStatus.COMPLETED.name());
            }
            item.setUpdateTime(now);
            itemMapper.updateById(item);
        }
        reversal.setRetryCount(defaultInt(reversal.getRetryCount()) + 1);
        reversal.setLastRetryTime(now);
        if (!completed) {
            reversal.setUpdateTime(now);
            reversalMapper.updateById(reversal);
            writeLog(reversal, requestKey, PurchaseReversalReviewOperationType.RETRY_INSUFFICIENT,
                PurchaseReversalDispositionStatus.PENDING_REVIEW.name(), note(bo), items, now);
            return actionResult(reversalNo, PurchaseReversalReviewOperationType.RETRY_INSUFFICIENT.name());
        }

        turnoverTaskService.cancelPendingByPurchase(tenantId, reversal.getMemberId(), reversal.getPurchaseOrderNo(), reversalNo, now);
        String finalOrderStatus = PurchaseReversalType.CHARGEBACK.name().equals(reversal.getReversalType())
            ? PurchaseOrderStatus.CHARGEBACK.name() : PurchaseOrderStatus.REFUNDED.name();
        order.setStatus(finalOrderStatus);
        order.setFailReason(null);
        order.setUpdateTime(now);
        orderMapper.updateById(order);
        finalizeCase(reversal, PurchaseReversalDispositionStatus.RECOVERY_COMPLETED, PurchaseReversalStatus.COMPLETED,
            note(bo), now, now, null);
        writeLog(reversal, requestKey, PurchaseReversalReviewOperationType.RETRY_COMPLETED,
            PurchaseReversalDispositionStatus.RECOVERY_COMPLETED.name(), note(bo), items, now);
        return actionResult(reversalNo, PurchaseReversalReviewOperationType.RETRY_COMPLETED.name());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReversalReviewActionResultVo acceptLoss(String reversalNo, PurchaseReversalReviewActionBo bo) {
        String tenantId = TenantHelper.getTenantId();
        String requestKey = requireRequestKey(bo);
        String reviewNote = requireNote(bo);
        PurchaseReversalReviewLog replay = logMapper.selectByRequestKey(tenantId, requestKey);
        if (replay != null) {
            if (!reversalNo.equals(replay.getReversalNo()) || !PurchaseReversalReviewOperationType.LOSS_ACCEPTED.name().equals(replay.getOperationType())) {
                throw new ServiceException(MessageUtils.message("payment.purchase.reversal.review.request.conflict"));
            }
            return actionResult(reversalNo, replay.getOperationType());
        }
        PurchaseReversal reversal = lockPending(tenantId, reversalNo);
        lockReviewOrder(tenantId, reversal);
        List<PurchaseReversalItem> items = requireItems(tenantId, reversalNo);
        Date now = new Date();
        WalletBatchDebitPreviewResult preview = walletCoreService.previewBatchDebit(walletRequest(reversal, items));
        Map<String, WalletBatchDebitPreviewLineResult> lines = new LinkedHashMap<>();
        preview.getLines().forEach(line -> lines.put(line.getCurrencyCode(), line));
        for (PurchaseReversalItem item : items) {
            WalletBatchDebitPreviewLineResult line = lines.get(item.getCurrencyCode());
            if (line == null) {
                throw new ServiceException(MessageUtils.message("payment.purchase.reversal.amount.invalid"));
            }
            item.setAvailableAmount(scale(line.getAvailableAmount()));
            item.setShortfallAmount(scale(line.getShortfallAmount()));
            item.setUpdateTime(now);
            itemMapper.updateById(item);
        }
        finalizeCase(reversal, PurchaseReversalDispositionStatus.LOSS_ACCEPTED, PurchaseReversalStatus.REVIEW_REQUIRED,
            reviewNote, now, null, reversal.getReviewReason());
        writeLog(reversal, requestKey, PurchaseReversalReviewOperationType.LOSS_ACCEPTED,
            PurchaseReversalDispositionStatus.LOSS_ACCEPTED.name(), reviewNote, items, now);
        return actionResult(reversalNo, PurchaseReversalReviewOperationType.LOSS_ACCEPTED.name());
    }

    private LambdaQueryWrapper<PurchaseReversal> buildQuery(String tenantId, PurchaseReversalReviewBo bo) {
        PurchaseReversalReviewBo query = bo == null ? new PurchaseReversalReviewBo() : bo;
        String disposition = StringUtils.blankToDefault(query.getDispositionStatus(),
            PurchaseReversalDispositionStatus.PENDING_REVIEW.name());
        LambdaQueryWrapper<PurchaseReversal> lqw = Wrappers.lambdaQuery();
        lqw.eq(PurchaseReversal::getTenantId, tenantId)
            .eq(PurchaseReversal::getDispositionStatus, disposition)
            .eq(StringUtils.isNotBlank(query.getReversalNo()), PurchaseReversal::getReversalNo, query.getReversalNo())
            .eq(StringUtils.isNotBlank(query.getPurchaseOrderNo()), PurchaseReversal::getPurchaseOrderNo, query.getPurchaseOrderNo())
            .eq(query.getMemberId() != null, PurchaseReversal::getMemberId, query.getMemberId())
            .eq(StringUtils.isNotBlank(query.getReversalType()), PurchaseReversal::getReversalType, query.getReversalType())
            .ge(query.getBeginTime() != null, PurchaseReversal::getCreateTime, query.getBeginTime())
            .le(query.getEndTime() != null, PurchaseReversal::getCreateTime, query.getEndTime())
            .orderByDesc(PurchaseReversal::getCreateTime);
        MemberNoQueryHelper.apply(lqw, query.getMemberNo(), "gl_purchase_reversal");
        return lqw;
    }

    private PurchaseReversalReviewVo toListVo(PurchaseReversal reversal) {
        PurchaseReversalReviewVo vo = BeanUtil.toBean(reversal, PurchaseReversalReviewVo.class);
        vo.setItems(items(reversal.getTenantId(), reversal.getReversalNo()));
        return vo;
    }

    private List<PurchaseReversalItemVo> items(String tenantId, String reversalNo) {
        return itemMapper.selectByReversalNo(tenantId, reversalNo).stream()
            .map(row -> BeanUtil.toBean(row, PurchaseReversalItemVo.class)).toList();
    }

    private PurchaseReversal lockPending(String tenantId, String reversalNo) {
        PurchaseReversal reversal = reversalMapper.selectByReversalNoForUpdate(tenantId, reversalNo);
        if (reversal == null) {
            throw new ServiceException(MessageUtils.message("payment.purchase.reversal.review.not.exists"));
        }
        if (!PurchaseReversalStatus.REVIEW_REQUIRED.name().equals(reversal.getStatus())
            || !PurchaseReversalDispositionStatus.PENDING_REVIEW.name().equals(reversal.getDispositionStatus())) {
            throw new ServiceException(MessageUtils.message("payment.purchase.reversal.review.resolved"));
        }
        return reversal;
    }

    private PurchaseOrder lockReviewOrder(String tenantId, PurchaseReversal reversal) {
        PurchaseOrder order = orderMapper.selectByOrderNoForUpdate(tenantId, reversal.getPurchaseOrderNo());
        String expected = PurchaseReversalType.CHARGEBACK.name().equals(reversal.getReversalType())
            ? PurchaseOrderStatus.CHARGEBACK_REVIEW.name() : PurchaseOrderStatus.REFUND_REVIEW.name();
        if (order == null || !expected.equals(order.getStatus())) {
            throw new ServiceException(MessageUtils.message("payment.purchase.reversal.review.state.invalid"));
        }
        return order;
    }

    private List<PurchaseReversalItem> requireItems(String tenantId, String reversalNo) {
        List<PurchaseReversalItem> items = itemMapper.selectByReversalNo(tenantId, reversalNo);
        if (items == null || items.isEmpty() || items.stream().anyMatch(item -> item.getRequiredAmount() == null
            || item.getRequiredAmount().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new ServiceException(MessageUtils.message("payment.purchase.reversal.amount.invalid"));
        }
        return items;
    }

    private WalletBatchDebitBo walletRequest(PurchaseReversal reversal, List<PurchaseReversalItem> items) {
        WalletBatchDebitBo request = new WalletBatchDebitBo();
        request.setTenantId(reversal.getTenantId());
        request.setMemberId(reversal.getMemberId());
        request.setBusinessNo(reversal.getReversalNo());
        request.setSourceType("PURCHASE_REVERSAL");
        request.setRemark("Purchase reversal review " + reversal.getReversalNo());
        List<WalletBatchDebitLineBo> lines = new ArrayList<>();
        for (PurchaseReversalItem item : items) {
            WalletBatchDebitLineBo line = new WalletBatchDebitLineBo();
            line.setCurrencyCode(item.getCurrencyCode());
            line.setAmount(item.getRequiredAmount());
            line.setIdempotencyKey("purchase-reversal-review:" + reversal.getReversalNo() + ":" + item.getCurrencyCode());
            lines.add(line);
        }
        request.setLines(lines);
        return request;
    }

    private void finalizeCase(PurchaseReversal reversal, PurchaseReversalDispositionStatus disposition,
                              PurchaseReversalStatus status, String reviewNote, Date now,
                              Date completedTime, String reviewReason) {
        int updated = reversalMapper.finalizeDisposition(reversal.getTenantId(), reversal.getReversalNo(),
            PurchaseReversalDispositionStatus.PENDING_REVIEW.name(), disposition.name(), status.name(),
            LoginHelper.getUserId(), LoginHelper.getUsername(), reviewNote, now, completedTime, reviewReason,
            reversal.getRetryCount(), reversal.getLastRetryTime());
        if (updated != 1) {
            throw new ServiceException(MessageUtils.message("payment.purchase.reversal.review.resolved"));
        }
        reversal.setStatus(status.name());
        reversal.setDispositionStatus(disposition.name());
        reversal.setReviewedBy(LoginHelper.getUserId());
        reversal.setReviewedName(LoginHelper.getUsername());
        reversal.setReviewNote(reviewNote);
        reversal.setResolvedTime(now);
    }

    private void writeLog(PurchaseReversal reversal, String requestKey,
                          PurchaseReversalReviewOperationType type, String afterStatus,
                          String reviewNote, List<PurchaseReversalItem> items, Date now) {
        PurchaseReversalReviewLog log = new PurchaseReversalReviewLog();
        log.setId(IdUtil.getSnowflakeNextId());
        log.setTenantId(reversal.getTenantId());
        log.setOperationNo("PRO" + IdUtil.getSnowflakeNextIdStr());
        log.setReversalId(reversal.getId());
        log.setReversalNo(reversal.getReversalNo());
        log.setRequestKey(requestKey);
        log.setOperationType(type.name());
        log.setBeforeStatus(PurchaseReversalDispositionStatus.PENDING_REVIEW.name());
        log.setAfterStatus(afterStatus);
        log.setOperatorId(LoginHelper.getUserId());
        log.setOperatorName(StringUtils.blankToDefault(LoginHelper.getUsername(), "unknown"));
        log.setReviewNote(reviewNote);
        log.setSnapshotJson(toJson(items.stream().map(item -> {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("currencyCode", item.getCurrencyCode());
            snapshot.put("requiredAmount", item.getRequiredAmount());
            snapshot.put("availableAmount", item.getAvailableAmount());
            snapshot.put("recoveredAmount", item.getRecoveredAmount());
            snapshot.put("shortfallAmount", item.getShortfallAmount());
            return snapshot;
        }).toList()));
        log.setCreateTime(now);
        logMapper.insert(log);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ServiceException(MessageUtils.message("system.error"));
        }
    }

    private PurchaseReversalReviewActionResultVo actionResult(String reversalNo, String operationType) {
        PurchaseReversalReviewActionResultVo result = new PurchaseReversalReviewActionResultVo();
        result.setOperationType(operationType);
        result.setCompleted(PurchaseReversalReviewOperationType.RETRY_COMPLETED.name().equals(operationType));
        result.setDetail(queryByReversalNo(reversalNo));
        result.setDispositionStatus(result.getDetail().getDispositionStatus());
        return result;
    }

    private String requireRequestKey(PurchaseReversalReviewActionBo bo) {
        String requestKey = bo == null ? null : StringUtils.trim(bo.getRequestKey());
        if (StringUtils.isBlank(requestKey)) {
            throw new ServiceException(MessageUtils.message("payment.purchase.reversal.review.request.required"));
        }
        return StringUtils.substring(requestKey, 0, 128);
    }

    private String requireNote(PurchaseReversalReviewActionBo bo) {
        String reviewNote = note(bo);
        if (StringUtils.isBlank(reviewNote)) {
            throw new ServiceException(MessageUtils.message("payment.purchase.reversal.review.note.required"));
        }
        return reviewNote;
    }

    private String note(PurchaseReversalReviewActionBo bo) {
        return bo == null ? null : StringUtils.substring(StringUtils.trim(bo.getReviewNote()), 0, 500);
    }

    private void requireWalletLine(Object line) {
        if (line == null) {
            throw new ServiceException(MessageUtils.message("payment.purchase.reversal.amount.invalid"));
        }
    }

    private BigDecimal scale(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(6, RoundingMode.HALF_UP);
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
