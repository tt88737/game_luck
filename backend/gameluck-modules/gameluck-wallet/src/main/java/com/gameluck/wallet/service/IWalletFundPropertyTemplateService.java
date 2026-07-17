package com.gameluck.wallet.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.wallet.domain.bo.WalletFundPropertyTemplateBo;
import com.gameluck.wallet.domain.vo.WalletFundPropertyTemplateVo;

import java.util.List;

public interface IWalletFundPropertyTemplateService {

    TableDataInfo<WalletFundPropertyTemplateVo> queryPageList(WalletFundPropertyTemplateBo bo, PageQuery pageQuery);

    WalletFundPropertyTemplateVo queryById(Long id);

    List<WalletFundPropertyTemplateVo> queryList(WalletFundPropertyTemplateBo bo);

    int insertByBo(WalletFundPropertyTemplateBo bo);

    int updateByBo(WalletFundPropertyTemplateBo bo);
}
