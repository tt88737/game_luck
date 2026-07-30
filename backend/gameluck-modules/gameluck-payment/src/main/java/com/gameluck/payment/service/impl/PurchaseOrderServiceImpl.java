package com.gameluck.payment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.mybatis.helper.MemberNoQueryHelper;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.domain.PurchaseReversal;
import com.gameluck.payment.domain.bo.PurchaseOrderBo;
import com.gameluck.payment.domain.bo.PurchasePaymentCallbackBo;
import com.gameluck.payment.domain.vo.PurchaseOrderDetailVo;
import com.gameluck.payment.domain.vo.PurchaseOrderGrantSnapshotVo;
import com.gameluck.payment.domain.vo.PurchaseOrderVo;
import com.gameluck.payment.domain.vo.PurchasePaymentEventVo;
import com.gameluck.payment.domain.vo.PurchaseReversalItemVo;
import com.gameluck.payment.domain.vo.PurchaseReversalVo;
import com.gameluck.payment.enums.PurchasePaymentEventType;
import com.gameluck.payment.mapper.PurchaseOrderGrantSnapshotMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.mapper.PurchasePaymentEventMapper;
import com.gameluck.payment.mapper.PurchaseReversalItemMapper;
import com.gameluck.payment.mapper.PurchaseReversalMapper;
import com.gameluck.payment.service.IPurchaseOrderService;
import com.gameluck.payment.service.IPurchasePaymentEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Purchase order admin service implementation.
 */
@RequiredArgsConstructor
@Service
public class PurchaseOrderServiceImpl implements IPurchaseOrderService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String MANUAL_PROVIDER = "MANUAL_ADMIN";

    private final PurchaseOrderMapper baseMapper;
    private final PurchaseOrderGrantSnapshotMapper snapshotMapper;
    private final PurchasePaymentEventMapper eventMapper;
    private final PurchaseReversalMapper reversalMapper;
    private final PurchaseReversalItemMapper reversalItemMapper;
    private final IPurchasePaymentEventService purchasePaymentEventService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public TableDataInfo<PurchaseOrderVo> queryPageList(PurchaseOrderBo bo, PageQuery pageQuery) {
        Page<PurchaseOrder> page = baseMapper.selectPage(pageQuery.build(), buildQueryWrapper(bo));
        List<PurchaseOrderVo> rows = page.getRecords().stream()
            .map(order -> BeanUtil.toBean(order, PurchaseOrderVo.class))
            .toList();
        MemberNoQueryHelper.fillMemberNo(jdbcTemplate, rows, PurchaseOrderVo::getMemberId, PurchaseOrderVo::setMemberNo);
        return new TableDataInfo<>(rows, page.getTotal());
    }

    @Override
    public PurchaseOrderDetailVo queryById(Long id) {
        PurchaseOrder order = loadOrder(id);
        PurchaseOrderDetailVo detail = BeanUtil.toBean(order, PurchaseOrderDetailVo.class);
        detail.setGrantSnapshots(snapshotMapper.selectByPurchaseOrderNo(order.getTenantId(), order.getPurchaseOrderNo()).stream()
            .map(snapshot -> BeanUtil.toBean(snapshot, PurchaseOrderGrantSnapshotVo.class))
            .toList());
        detail.setPaymentEvents(eventMapper.selectByPurchaseOrderNo(order.getTenantId(), order.getPurchaseOrderNo()).stream()
            .map(event -> BeanUtil.toBean(event, PurchasePaymentEventVo.class))
            .toList());
        PurchaseReversal reversal = reversalMapper.selectByPurchaseOrderNo(order.getTenantId(), order.getPurchaseOrderNo());
        if (reversal != null) {
            PurchaseReversalVo reversalVo = BeanUtil.toBean(reversal, PurchaseReversalVo.class);
            reversalVo.setItems(reversalItemMapper.selectByReversalNo(order.getTenantId(), reversal.getReversalNo()).stream()
                .map(item -> BeanUtil.toBean(item, PurchaseReversalItemVo.class))
                .toList());
            detail.setReversal(reversalVo);
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDetailVo markFailed(Long id, String reason) {
        return applyManualEvent(id, PurchasePaymentEventType.PAY_FAILED, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDetailVo cancel(Long id, String reason) {
        return applyManualEvent(id, PurchasePaymentEventType.CANCELLED, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDetailVo refund(Long id, String reason) {
        return applyManualEvent(id, PurchasePaymentEventType.REFUNDED, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDetailVo chargeback(Long id, String reason) {
        return applyManualEvent(id, PurchasePaymentEventType.CHARGEBACK, reason);
    }

    private PurchaseOrderDetailVo applyManualEvent(Long id, PurchasePaymentEventType eventType, String reason) {
        String normalizedReason = requireReason(reason);
        PurchaseOrder order = loadOrder(id);
        PurchasePaymentCallbackBo command = PurchasePaymentCallbackBo.builder()
            .tenantId(StringUtils.blankToDefault(order.getTenantId(), DEFAULT_TENANT_ID))
            .eventKey(manualEventKey(eventType, order.getPurchaseOrderNo()))
            .purchaseOrderNo(order.getPurchaseOrderNo())
            .providerCode(MANUAL_PROVIDER)
            .providerOrderNo(StringUtils.blankToDefault(order.getProviderOrderNo(), order.getPurchaseOrderNo()))
            .eventType(eventType)
            .requestBody(manualRequestBody(eventType, normalizedReason))
            .failReason(normalizedReason)
            .build();
        PurchaseOrder updated = purchasePaymentEventService.applyEvent(command);
        return queryById(updated.getId());
    }

    private LambdaQueryWrapper<PurchaseOrder> buildQueryWrapper(PurchaseOrderBo bo) {
        LambdaQueryWrapper<PurchaseOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), PurchaseOrder::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getPurchaseOrderNo()), PurchaseOrder::getPurchaseOrderNo, bo.getPurchaseOrderNo());
        lqw.eq(bo.getMemberId() != null, PurchaseOrder::getMemberId, bo.getMemberId());
        MemberNoQueryHelper.apply(lqw, bo.getMemberNo(), "gl_purchase_order");
        lqw.eq(bo.getOfferId() != null, PurchaseOrder::getOfferId, bo.getOfferId());
        lqw.eq(StringUtils.isNotBlank(bo.getOfferNo()), PurchaseOrder::getOfferNo, bo.getOfferNo());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), PurchaseOrder::getStatus, bo.getStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getProviderCode()), PurchaseOrder::getProviderCode, bo.getProviderCode());
        lqw.eq(StringUtils.isNotBlank(bo.getProviderOrderNo()), PurchaseOrder::getProviderOrderNo, bo.getProviderOrderNo());
        lqw.eq(StringUtils.isNotBlank(bo.getPaymentSessionNo()), PurchaseOrder::getPaymentSessionNo, bo.getPaymentSessionNo());
        lqw.eq(StringUtils.isNotBlank(bo.getIdempotencyKey()), PurchaseOrder::getIdempotencyKey, bo.getIdempotencyKey());
        lqw.ge(bo.getBeginTime() != null, PurchaseOrder::getPaidTime, bo.getBeginTime());
        lqw.le(bo.getEndTime() != null, PurchaseOrder::getPaidTime, bo.getEndTime());
        lqw.orderByDesc(PurchaseOrder::getCreateTime);
        return lqw;
    }

    private PurchaseOrder loadOrder(Long id) {
        PurchaseOrder order = baseMapper.selectById(id);
        if (order == null) {
            throw new ServiceException(MessageUtils.message("payment.purchase.order.not.exists"));
        }
        return order;
    }

    private String requireReason(String reason) {
        String trimmed = StringUtils.trim(reason);
        if (StringUtils.isBlank(trimmed)) {
            throw new ServiceException(MessageUtils.message("payment.purchase.manual.reason.required"));
        }
        return StringUtils.substring(trimmed, 0, 500);
    }

    private String manualEventKey(PurchasePaymentEventType eventType, String purchaseOrderNo) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        return "purchase:manual:" + eventType.name().toLowerCase() + ":" + purchaseOrderNo + ":" + timestamp;
    }

    private String manualRequestBody(PurchasePaymentEventType eventType, String reason) {
        return "{\"source\":\"ADMIN\",\"action\":\"" + eventType.name() + "\",\"reason\":\"" + jsonEscape(reason) + "\"}";
    }

    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

}
