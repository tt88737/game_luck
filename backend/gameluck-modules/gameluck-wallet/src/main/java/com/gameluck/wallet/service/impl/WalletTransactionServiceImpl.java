package com.gameluck.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.mybatis.helper.MemberNoQueryHelper;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletTransactionBo;
import com.gameluck.wallet.domain.vo.WalletTransactionVo;
import com.gameluck.wallet.mapper.WalletTransactionMapper;
import com.gameluck.wallet.service.IWalletTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Wallet transaction service implementation.
 */
@RequiredArgsConstructor
@Service
public class WalletTransactionServiceImpl implements IWalletTransactionService {

    private final WalletTransactionMapper baseMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public TableDataInfo<WalletTransactionVo> queryPageList(WalletTransactionBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WalletTransaction> lqw = buildQueryWrapper(bo);
        Page<WalletTransactionVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        MemberNoQueryHelper.fillMemberNo(jdbcTemplate, page.getRecords(), WalletTransactionVo::getMemberId, WalletTransactionVo::setMemberNo);
        return TableDataInfo.build(page);
    }

    @Override
    public WalletTransactionVo queryById(Long id) {
        return MemberNoQueryHelper.fillMemberNo(jdbcTemplate, baseMapper.selectVoById(id), WalletTransactionVo::getMemberId, WalletTransactionVo::setMemberNo);
    }

    @Override
    public List<WalletTransactionVo> queryList(WalletTransactionBo bo) {
        LambdaQueryWrapper<WalletTransaction> lqw = buildQueryWrapper(bo);
        List<WalletTransactionVo> rows = baseMapper.selectVoList(lqw);
        MemberNoQueryHelper.fillMemberNo(jdbcTemplate, rows, WalletTransactionVo::getMemberId, WalletTransactionVo::setMemberNo);
        return rows;
    }

    private LambdaQueryWrapper<WalletTransaction> buildQueryWrapper(WalletTransactionBo bo) {
        LambdaQueryWrapper<WalletTransaction> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), WalletTransaction::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getTransactionNo()), WalletTransaction::getTransactionNo, bo.getTransactionNo());
        lqw.eq(bo.getMemberId() != null, WalletTransaction::getMemberId, bo.getMemberId());
        MemberNoQueryHelper.apply(lqw, bo.getMemberNo(), "gl_wallet_transaction");
        lqw.eq(StringUtils.isNotBlank(bo.getCurrencyCode()), WalletTransaction::getCurrencyCode, bo.getCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getOperation()), WalletTransaction::getOperation, bo.getOperation());
        lqw.eq(StringUtils.isNotBlank(bo.getSourceType()), WalletTransaction::getSourceType, bo.getSourceType());
        lqw.eq(StringUtils.isNotBlank(bo.getBusinessNo()), WalletTransaction::getBusinessNo, bo.getBusinessNo());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), WalletTransaction::getStatus, bo.getStatus());
        lqw.ge(bo.getBeginTime() != null, WalletTransaction::getCreateTime, bo.getBeginTime());
        lqw.le(bo.getEndTime() != null, WalletTransaction::getCreateTime, bo.getEndTime());
        lqw.orderByDesc(WalletTransaction::getCreateTime);
        return lqw;
    }
}
