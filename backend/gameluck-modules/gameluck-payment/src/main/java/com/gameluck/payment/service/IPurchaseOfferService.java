package com.gameluck.payment.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.payment.domain.PurchaseOfferGrantItem;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.domain.bo.PurchaseOfferBo;
import com.gameluck.payment.domain.vo.PurchaseOfferVo;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.payment.domain.PurchaseOrderGrantSnapshot;

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

    List<PurchaseOrderGrantSnapshot> prepareOrderGrantSnapshots(PurchaseOrder order, List<PurchaseOfferGrantItem> items);

    List<PurchaseOrderGrantSnapshot> orderGrantSnapshots(PurchaseOrder order);

    List<PurchaseOrderGrantSnapshot> orderGrantSnapshotsForUpdate(PurchaseOrder order);

    List<WalletCreditBo> creditsFromOrderSnapshots(PurchaseOrder order);
}
