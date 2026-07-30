package com.gameluck.payment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.constant.SystemConstants;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.domain.PurchaseOffer;
import com.gameluck.payment.domain.PurchaseOfferGrantItem;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.domain.PurchaseOrderGrantSnapshot;
import com.gameluck.payment.domain.bo.PurchaseOfferBo;
import com.gameluck.payment.domain.bo.PurchaseOfferGrantItemBo;
import com.gameluck.payment.domain.vo.PurchaseOfferVo;
import com.gameluck.payment.mapper.PurchaseOfferGrantItemMapper;
import com.gameluck.payment.mapper.PurchaseOfferMapper;
import com.gameluck.payment.mapper.PurchaseOrderGrantSnapshotMapper;
import com.gameluck.payment.service.IPurchaseOfferService;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Purchase offer service implementation.
 */
@RequiredArgsConstructor
@Service
public class PurchaseOfferServiceImpl implements IPurchaseOfferService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String DEFAULT_OFFER_TYPE = "STANDARD";
    private static final String DEFAULT_PAY_CURRENCY = "USD";
    private static final String DEFAULT_SCOPE_ALL = "ALL";
    private static final String DEFAULT_LIMIT_NONE = "NONE";
    private static final String DEFAULT_STACKABLE_NO = "1";
    private static final String DEFAULT_STATUS_DISABLED = "1";
    private static final String WAGERING_NONE = "NONE";
    private static final String WAGERING_FIXED = "FIXED";
    private static final String WAGERING_MULTIPLIER = "MULTIPLIER";
    private static final String WAGERING_COMBINED_MULTIPLIER = "COMBINED_MULTIPLIER";
    private static final String SOURCE_TYPE_PURCHASE = "PURCHASE";
    private static final int MONEY_SCALE = 6;

    private final PurchaseOfferMapper baseMapper;
    private final PurchaseOfferGrantItemMapper grantItemMapper;
    private final PurchaseOrderGrantSnapshotMapper snapshotMapper;

    @Override
    public TableDataInfo<PurchaseOfferVo> queryPageList(PurchaseOfferBo bo, PageQuery pageQuery) {
        Page<PurchaseOfferVo> page = baseMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        fillGrantItems(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public PurchaseOfferVo queryById(Long id) {
        PurchaseOfferVo vo = baseMapper.selectVoById(id);
        fillGrantItems(vo == null ? List.of() : List.of(vo));
        return vo;
    }

    @Override
    public List<PurchaseOfferVo> queryList(PurchaseOfferBo bo) {
        List<PurchaseOfferVo> rows = baseMapper.selectVoList(buildQueryWrapper(bo));
        fillGrantItems(rows);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertByBo(PurchaseOfferBo bo) {
        validateOffer(bo);
        Date now = new Date();
        String tenantId = currentTenantId();
        PurchaseOffer offer = BeanUtil.toBean(bo, PurchaseOffer.class);
        offer.setId(IdUtil.getSnowflakeNextId());
        offer.setTenantId(tenantId);
        offer.setOfferNo(StringUtils.blankToDefault(bo.getOfferNo(), "PO" + IdUtil.getSnowflakeNextIdStr()));
        offer.setOfferType(StringUtils.blankToDefault(bo.getOfferType(), DEFAULT_OFFER_TYPE));
        offer.setPayCurrencyCode(StringUtils.blankToDefault(bo.getPayCurrencyCode(), DEFAULT_PAY_CURRENCY));
        offer.setPayAmount(normalizeAmount(bo.getPayAmount()));
        offer.setUserScopeType(StringUtils.blankToDefault(bo.getUserScopeType(), DEFAULT_SCOPE_ALL));
        offer.setRegionScopeType(StringUtils.blankToDefault(bo.getRegionScopeType(), DEFAULT_SCOPE_ALL));
        offer.setPurchaseLimitType(StringUtils.blankToDefault(bo.getPurchaseLimitType(), DEFAULT_LIMIT_NONE));
        offer.setStackable(StringUtils.blankToDefault(bo.getStackable(), DEFAULT_STACKABLE_NO));
        offer.setStatus(StringUtils.blankToDefault(bo.getStatus(), DEFAULT_STATUS_DISABLED));
        offer.setSortOrder(bo.getSortOrder() == null ? 0 : bo.getSortOrder());
        offer.setVersion(0);
        offer.setDelFlag(SystemConstants.NORMAL);
        offer.setCreateTime(now);
        offer.setUpdateTime(now);
        int rows = baseMapper.insert(offer);
        insertGrantItems(tenantId, offer.getId(), bo.getGrantItems());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(PurchaseOfferBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException(MessageUtils.message("payment.purchase.offer.id.required"));
        }
        validateOffer(bo);
        PurchaseOffer update = BeanUtil.toBean(bo, PurchaseOffer.class);
        update.setPayAmount(normalizeAmount(bo.getPayAmount()));
        update.setUpdateTime(new Date());
        int rows = baseMapper.updateById(update);
        grantItemMapper.delete(Wrappers.<PurchaseOfferGrantItem>lambdaQuery().eq(PurchaseOfferGrantItem::getOfferId, bo.getId()));
        insertGrantItems(currentTenantId(), bo.getId(), bo.getGrantItems());
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PurchaseOrderGrantSnapshot> prepareOrderGrantSnapshots(PurchaseOrder order, List<PurchaseOfferGrantItem> items) {
        List<WalletCreditBo> credits = buildWalletCreditsForPaidOrder(order, items);
        List<PurchaseOrderGrantSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            PurchaseOfferGrantItem item = items.get(i);
            WalletCreditBo credit = credits.get(i);
            PurchaseOrderGrantSnapshot snapshot = new PurchaseOrderGrantSnapshot();
            snapshot.setId(IdUtil.getSnowflakeNextId());
            snapshot.setTenantId(order.getTenantId());
            snapshot.setPurchaseOrderId(order.getId());
            snapshot.setPurchaseOrderNo(order.getPurchaseOrderNo());
            snapshot.setMemberId(order.getMemberId());
            snapshot.setGrantType(item.getGrantType());
            snapshot.setCurrencyCode(item.getCurrencyCode());
            snapshot.setGrantAmount(item.getGrantAmount());
            snapshot.setFundPropertyCode(item.getFundPropertyCode());
            snapshot.setWageringMode(item.getWageringMode());
            snapshot.setWageringMultiplier(item.getWageringMultiplier());
            snapshot.setWageringExpireDays(item.getWageringExpireDays());
            snapshot.setRequiredTurnover(credit.getTurnoverRequiredAmount());
            snapshot.setGameScopeType(StringUtils.blankToDefault(item.getGameScopeType(), DEFAULT_SCOPE_ALL));
            snapshot.setGameScopeValue(item.getGameScopeValue());
            snapshot.setRuleSnapshot(credit.getRuleSnapshot());
            snapshotMapper.insert(snapshot);
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    @Override
    public List<PurchaseOrderGrantSnapshot> orderGrantSnapshots(PurchaseOrder order) {
        return snapshotMapper.selectByPurchaseOrderNo(order.getTenantId(), order.getPurchaseOrderNo());
    }

    @Override
    public List<PurchaseOrderGrantSnapshot> orderGrantSnapshotsForUpdate(PurchaseOrder order) {
        return snapshotMapper.selectByPurchaseOrderNoForUpdate(order.getTenantId(), order.getPurchaseOrderNo());
    }

    @Override
    public List<WalletCreditBo> creditsFromOrderSnapshots(PurchaseOrder order) {
        List<PurchaseOrderGrantSnapshot> snapshots = orderGrantSnapshots(order);
        if (snapshots.isEmpty()) throw new ServiceException(MessageUtils.message("payment.purchase.reversal.snapshot.missing"));
        List<WalletCreditBo> credits = new ArrayList<>();
        for (PurchaseOrderGrantSnapshot snapshot : snapshots) {
            WalletCreditBo credit = new WalletCreditBo();
            credit.setIdempotencyKey("purchase:" + order.getPurchaseOrderNo() + ":" + snapshot.getId());
            credit.setMemberId(order.getMemberId()); credit.setCurrencyCode(snapshot.getCurrencyCode());
            credit.setAmount(snapshot.getGrantAmount()); credit.setSourceType(SOURCE_TYPE_PURCHASE);
            credit.setBusinessNo(order.getPurchaseOrderNo()); credit.setFundPropertyCode(snapshot.getFundPropertyCode());
            credit.setTurnoverRequiredAmount(snapshot.getRequiredTurnover()); credit.setRequiredTurnover(snapshot.getRequiredTurnover());
            credit.setTurnoverMultiplier(snapshot.getWageringMultiplier() == null ? BigDecimal.ZERO : snapshot.getWageringMultiplier());
            credit.setTurnoverExpireTime(resolveTurnoverExpireTime(snapshot.getWageringExpireDays()));
            credit.setGameScopeType(snapshot.getGameScopeType()); credit.setGameScopeValue(snapshot.getGameScopeValue());
            credit.setSourceId(order.getId() == null ? null : order.getId().toString()); credit.setRuleSnapshot(snapshot.getRuleSnapshot());
            credits.add(credit);
        }
        return credits;
    }

    public List<WalletCreditBo> buildWalletCreditsForPaidOrder(PurchaseOrder order, List<PurchaseOfferGrantItem> items) {
        if (order == null || StringUtils.isBlank(order.getPurchaseOrderNo()) || order.getMemberId() == null) {
            throw new ServiceException(MessageUtils.message("payment.purchase.order.incomplete"));
        }
        if (items == null || items.isEmpty()) {
            throw new ServiceException(MessageUtils.message("payment.purchase.order.grant.required"));
        }
        List<WalletCreditBo> credits = new ArrayList<>();
        for (PurchaseOfferGrantItem item : items) {
            BigDecimal requiredTurnover = calculateRequiredTurnover(item);
            WalletCreditBo credit = new WalletCreditBo();
            credit.setIdempotencyKey("purchase:credit:" + order.getPurchaseOrderNo() + ":" + item.getCurrencyCode() + ":" + item.getGrantType());
            credit.setMemberId(order.getMemberId());
            credit.setCurrencyCode(item.getCurrencyCode());
            credit.setAmount(item.getGrantAmount());
            credit.setSourceType(SOURCE_TYPE_PURCHASE);
            credit.setBusinessNo(order.getPurchaseOrderNo());
            credit.setFundPropertyCode(item.getFundPropertyCode());
            credit.setTurnoverRequiredAmount(requiredTurnover);
            credit.setRequiredTurnover(requiredTurnover);
            credit.setTurnoverMultiplier(item.getWageringMultiplier() == null ? BigDecimal.ZERO : item.getWageringMultiplier());
            credit.setGameScopeType(StringUtils.blankToDefault(item.getGameScopeType(), DEFAULT_SCOPE_ALL));
            credit.setGameScopeValue(item.getGameScopeValue());
            credit.setSourceId(order.getId() == null ? null : order.getId().toString());
            credit.setRuleSnapshot(buildRuleSnapshot(item, requiredTurnover));
            credit.setTurnoverExpireTime(resolveTurnoverExpireTime(item.getWageringExpireDays()));
            credits.add(credit);
        }
        return credits;
    }

    private LambdaQueryWrapper<PurchaseOffer> buildQueryWrapper(PurchaseOfferBo bo) {
        LambdaQueryWrapper<PurchaseOffer> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), PurchaseOffer::getTenantId, bo.getTenantId());
        lqw.like(StringUtils.isNotBlank(bo.getOfferName()), PurchaseOffer::getOfferName, bo.getOfferName());
        lqw.eq(StringUtils.isNotBlank(bo.getOfferNo()), PurchaseOffer::getOfferNo, bo.getOfferNo());
        lqw.eq(StringUtils.isNotBlank(bo.getOfferType()), PurchaseOffer::getOfferType, bo.getOfferType());
        lqw.eq(StringUtils.isNotBlank(bo.getPayCurrencyCode()), PurchaseOffer::getPayCurrencyCode, bo.getPayCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), PurchaseOffer::getStatus, bo.getStatus());
        lqw.ge(bo.getBeginTime() != null, PurchaseOffer::getCreateTime, bo.getBeginTime());
        lqw.le(bo.getEndQueryTime() != null, PurchaseOffer::getCreateTime, bo.getEndQueryTime());
        lqw.orderByAsc(PurchaseOffer::getSortOrder).orderByDesc(PurchaseOffer::getCreateTime);
        return lqw;
    }

    private void validateOffer(PurchaseOfferBo bo) {
        if (StringUtils.isBlank(bo.getOfferName())) {
            throw new ServiceException(MessageUtils.message("payment.purchase.offer.name.required"));
        }
        if (bo.getGrantItems() == null || bo.getGrantItems().isEmpty()) {
            throw new ServiceException(MessageUtils.message("payment.purchase.offer.grant.required"));
        }
    }

    private void fillGrantItems(List<PurchaseOfferVo> offers) {
        if (offers == null || offers.isEmpty()) {
            return;
        }
        List<Long> offerIds = offers.stream().map(PurchaseOfferVo::getId).toList();
        List<PurchaseOfferGrantItem> items = grantItemMapper.selectList(Wrappers.<PurchaseOfferGrantItem>lambdaQuery()
            .in(PurchaseOfferGrantItem::getOfferId, offerIds)
            .orderByAsc(PurchaseOfferGrantItem::getSortOrder));
        Map<Long, List<PurchaseOfferGrantItem>> grouped = items.stream().collect(Collectors.groupingBy(PurchaseOfferGrantItem::getOfferId));
        for (PurchaseOfferVo offer : offers) {
            offer.setGrantItems(BeanUtil.copyToList(grouped.getOrDefault(offer.getId(), List.of()), com.gameluck.payment.domain.vo.PurchaseOfferGrantItemVo.class));
        }
    }

    private void insertGrantItems(String tenantId, Long offerId, List<PurchaseOfferGrantItemBo> items) {
        int index = 0;
        for (PurchaseOfferGrantItemBo itemBo : items) {
            PurchaseOfferGrantItem item = BeanUtil.toBean(itemBo, PurchaseOfferGrantItem.class);
            item.setId(IdUtil.getSnowflakeNextId());
            item.setTenantId(tenantId);
            item.setOfferId(offerId);
            item.setGrantAmount(normalizeAmount(itemBo.getGrantAmount()));
            item.setFundPropertyCode(resolveFundPropertyCode(itemBo));
            normalizeWagering(item);
            item.setGameScopeType(StringUtils.blankToDefault(itemBo.getGameScopeType(), DEFAULT_SCOPE_ALL));
            item.setWageringExpireDays(itemBo.getWageringExpireDays() == null ? 0 : itemBo.getWageringExpireDays());
            item.setSortOrder(itemBo.getSortOrder() == null ? index * 10 : itemBo.getSortOrder());
            grantItemMapper.insert(item);
            index++;
        }
    }

    private void normalizeWagering(PurchaseOfferGrantItem item) {
        String mode = StringUtils.blankToDefault(item.getWageringMode(), WAGERING_NONE);
        item.setWageringMode(mode);
        if (WAGERING_NONE.equals(mode)) {
            item.setWageringRequiredAmount(BigDecimal.ZERO);
            item.setWageringMultiplier(BigDecimal.ZERO);
            return;
        }
        if (WAGERING_FIXED.equals(mode)) {
            item.setWageringRequiredAmount(normalizeAmount(item.getWageringRequiredAmount()));
            item.setWageringMultiplier(BigDecimal.ZERO);
            return;
        }
        if (WAGERING_MULTIPLIER.equals(mode)) {
            item.setWageringRequiredAmount(BigDecimal.ZERO);
            item.setWageringMultiplier(item.getWageringMultiplier() == null ? BigDecimal.ZERO : item.getWageringMultiplier());
            return;
        }
        throw new ServiceException(MessageUtils.message("payment.purchase.wagering.mode.unsupported"));
    }

    private BigDecimal calculateRequiredTurnover(PurchaseOfferGrantItem item) {
        String mode = StringUtils.blankToDefault(item.getWageringMode(), WAGERING_NONE);
        if (WAGERING_NONE.equals(mode)) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        if (WAGERING_FIXED.equals(mode)) {
            return normalizeNonNegativeAmount(item.getWageringRequiredAmount());
        }
        if (WAGERING_MULTIPLIER.equals(mode)) {
            BigDecimal multiplier = item.getWageringMultiplier() == null ? BigDecimal.ZERO : item.getWageringMultiplier();
            return item.getGrantAmount().multiply(multiplier).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        if (WAGERING_COMBINED_MULTIPLIER.equals(mode)) {
            throw new ServiceException(MessageUtils.message("payment.purchase.wagering.combined.unsupported"));
        }
        throw new ServiceException(MessageUtils.message("payment.purchase.wagering.mode.unknown"));
    }

    private Date resolveTurnoverExpireTime(Integer expireDays) {
        if (expireDays == null || expireDays <= 0) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, expireDays);
        return calendar.getTime();
    }

    private String buildRuleSnapshot(PurchaseOfferGrantItem item, BigDecimal requiredTurnover) {
        return "{\"wageringMode\":\"" + StringUtils.blankToDefault(item.getWageringMode(), WAGERING_NONE)
            + "\",\"requiredTurnover\":\"" + requiredTurnover
            + "\",\"wageringMultiplier\":\"" + (item.getWageringMultiplier() == null ? BigDecimal.ZERO : item.getWageringMultiplier())
            + "\",\"gameScopeType\":\"" + StringUtils.blankToDefault(item.getGameScopeType(), DEFAULT_SCOPE_ALL)
            + "\"}";
    }

    private String resolveFundPropertyCode(PurchaseOfferGrantItemBo item) {
        String grantType = item.getGrantType();
        String currencyCode = item.getCurrencyCode();
        if ("PURCHASE_GRANT".equals(grantType) && "GC".equals(currencyCode)) {
            return "PURCHASE_GRANT_GC";
        }
        if ("PURCHASE_BONUS".equals(grantType) && "SC".equals(currencyCode)) {
            return "PURCHASE_BONUS_SC";
        }
        return grantType + "_" + currencyCode;
    }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(MessageUtils.message("payment.purchase.amount.positive"));
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeNonNegativeAmount(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException(MessageUtils.message("payment.purchase.amount.nonnegative"));
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
