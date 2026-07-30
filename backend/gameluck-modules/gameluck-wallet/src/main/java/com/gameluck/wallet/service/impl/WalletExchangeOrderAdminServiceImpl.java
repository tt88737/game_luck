package com.gameluck.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.mybatis.helper.MemberNoQueryHelper;
import com.gameluck.wallet.domain.WalletExchangeOrder;
import com.gameluck.wallet.domain.bo.WalletExchangeOrderBo;
import com.gameluck.wallet.domain.vo.WalletExchangeOrderVo;
import com.gameluck.wallet.mapper.WalletExchangeOrderMapper;
import com.gameluck.wallet.service.IWalletExchangeOrderAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Wallet exchange order admin query service implementation.
 */
@RequiredArgsConstructor
@Service
public class WalletExchangeOrderAdminServiceImpl implements IWalletExchangeOrderAdminService {

    private final WalletExchangeOrderMapper baseMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public TableDataInfo<WalletExchangeOrderVo> queryPageList(WalletExchangeOrderBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WalletExchangeOrder> lqw = buildQueryWrapper(bo);
        Page<WalletExchangeOrderVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        MemberNoQueryHelper.fillMemberNo(jdbcTemplate, page.getRecords(), WalletExchangeOrderVo::getMemberId, WalletExchangeOrderVo::setMemberNo);
        return TableDataInfo.build(page);
    }

    @Override
    public WalletExchangeOrderVo queryById(Long id) {
        return MemberNoQueryHelper.fillMemberNo(jdbcTemplate, baseMapper.selectVoById(id), WalletExchangeOrderVo::getMemberId, WalletExchangeOrderVo::setMemberNo);
    }

    @Override
    public List<WalletExchangeOrderVo> queryList(WalletExchangeOrderBo bo) {
        List<WalletExchangeOrderVo> rows = baseMapper.selectVoList(buildQueryWrapper(bo));
        MemberNoQueryHelper.fillMemberNo(jdbcTemplate, rows, WalletExchangeOrderVo::getMemberId, WalletExchangeOrderVo::setMemberNo);
        return rows;
    }

    private LambdaQueryWrapper<WalletExchangeOrder> buildQueryWrapper(WalletExchangeOrderBo bo) {
        LambdaQueryWrapper<WalletExchangeOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), WalletExchangeOrder::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getExchangeOrderNo()), WalletExchangeOrder::getExchangeOrderNo, bo.getExchangeOrderNo());
        lqw.eq(bo.getMemberId() != null, WalletExchangeOrder::getMemberId, bo.getMemberId());
        MemberNoQueryHelper.apply(lqw, bo.getMemberNo(), "gl_wallet_exchange_order");
        lqw.eq(bo.getExchangeRuleId() != null, WalletExchangeOrder::getExchangeRuleId, bo.getExchangeRuleId());
        lqw.eq(StringUtils.isNotBlank(bo.getFromCurrencyCode()), WalletExchangeOrder::getFromCurrencyCode, bo.getFromCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getToCurrencyCode()), WalletExchangeOrder::getToCurrencyCode, bo.getToCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getDebitTransactionNo()), WalletExchangeOrder::getDebitTransactionNo, bo.getDebitTransactionNo());
        lqw.eq(StringUtils.isNotBlank(bo.getCreditTransactionNo()), WalletExchangeOrder::getCreditTransactionNo, bo.getCreditTransactionNo());
        lqw.eq(StringUtils.isNotBlank(bo.getTurnoverTaskNo()), WalletExchangeOrder::getTurnoverTaskNo, bo.getTurnoverTaskNo());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), WalletExchangeOrder::getStatus, bo.getStatus());
        lqw.ge(bo.getBeginTime() != null, WalletExchangeOrder::getCreateTime, bo.getBeginTime());
        lqw.le(bo.getEndTime() != null, WalletExchangeOrder::getCreateTime, bo.getEndTime());
        lqw.orderByDesc(WalletExchangeOrder::getCreateTime);
        return lqw;
    }
}
