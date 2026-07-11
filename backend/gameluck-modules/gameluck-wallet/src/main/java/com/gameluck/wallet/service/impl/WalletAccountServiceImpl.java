package com.gameluck.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.mybatis.helper.MemberNoQueryHelper;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.wallet.domain.WalletAccount;
import com.gameluck.wallet.domain.bo.WalletAccountBo;
import com.gameluck.wallet.domain.vo.WalletAccountVo;
import com.gameluck.wallet.mapper.WalletAccountMapper;
import com.gameluck.wallet.service.IWalletAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Wallet account service implementation.
 */
@RequiredArgsConstructor
@Service
public class WalletAccountServiceImpl implements IWalletAccountService {

    private final WalletAccountMapper baseMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public TableDataInfo<WalletAccountVo> queryPageList(WalletAccountBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WalletAccount> lqw = buildQueryWrapper(bo);
        Page<WalletAccountVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        MemberNoQueryHelper.fillMemberNo(jdbcTemplate, page.getRecords(), WalletAccountVo::getMemberId, WalletAccountVo::setMemberNo);
        return TableDataInfo.build(page);
    }

    @Override
    public WalletAccountVo queryById(Long id) {
        return MemberNoQueryHelper.fillMemberNo(jdbcTemplate, baseMapper.selectVoById(id), WalletAccountVo::getMemberId, WalletAccountVo::setMemberNo);
    }

    @Override
    public List<WalletAccountVo> queryList(WalletAccountBo bo) {
        LambdaQueryWrapper<WalletAccount> lqw = buildQueryWrapper(bo);
        List<WalletAccountVo> rows = baseMapper.selectVoList(lqw);
        MemberNoQueryHelper.fillMemberNo(jdbcTemplate, rows, WalletAccountVo::getMemberId, WalletAccountVo::setMemberNo);
        return rows;
    }

    private LambdaQueryWrapper<WalletAccount> buildQueryWrapper(WalletAccountBo bo) {
        LambdaQueryWrapper<WalletAccount> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), WalletAccount::getTenantId, bo.getTenantId());
        lqw.eq(bo.getMemberId() != null, WalletAccount::getMemberId, bo.getMemberId());
        MemberNoQueryHelper.apply(lqw, bo.getMemberNo(), "gl_wallet_account");
        lqw.eq(StringUtils.isNotBlank(bo.getCurrencyCode()), WalletAccount::getCurrencyCode, bo.getCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), WalletAccount::getStatus, bo.getStatus());
        lqw.orderByDesc(WalletAccount::getCreateTime);
        return lqw;
    }
}
