package com.gameluck.payment.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.payment.domain.bo.PurchaseOfferBo;
import com.gameluck.payment.domain.vo.PurchaseOfferVo;

import java.util.List;

/**
 * Purchase offer service.
 */
public interface IPurchaseOfferService {

    TableDataInfo<PurchaseOfferVo> queryPageList(PurchaseOfferBo bo, PageQuery pageQuery);

    PurchaseOfferVo queryById(Long id);

    List<PurchaseOfferVo> queryList(PurchaseOfferBo bo);

    int insertByBo(PurchaseOfferBo bo);

    Boolean updateByBo(PurchaseOfferBo bo);
}
