package com.gameluck.payment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.constant.SystemConstants;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.domain.PurchaseOffer;
import com.gameluck.payment.domain.PurchaseOfferGrantItem;
import com.gameluck.payment.domain.bo.PurchaseOfferBo;
import com.gameluck.payment.domain.bo.PurchaseOfferGrantItemBo;
import com.gameluck.payment.domain.vo.PurchaseOfferVo;
import com.gameluck.payment.mapper.PurchaseOfferGrantItemMapper;
import com.gameluck.payment.mapper.PurchaseOfferMapper;
import com.gameluck.payment.service.IPurchaseOfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private static final int MONEY_SCALE = 6;

    private final PurchaseOfferMapper baseMapper;
    private final PurchaseOfferGrantItemMapper grantItemMapper;

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
            throw new ServiceException("购买产品ID不能为空");
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
            throw new ServiceException("购买产品名称不能为空");
        }
        if (bo.getGrantItems() == null || bo.getGrantItems().isEmpty()) {
            throw new ServiceException("至少配置一个发放项");
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
        throw new ServiceException("暂不支持该流水模式");
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
            throw new ServiceException("金额必须大于0");
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
