package com.gameluck.payment.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.payment.domain.bo.PurchaseOrderBo;
import com.gameluck.payment.domain.vo.PurchaseOrderDetailVo;
import com.gameluck.payment.domain.vo.PurchaseOrderVo;

/**
 * Purchase order admin service.
 */
public interface IPurchaseOrderService {

    TableDataInfo<PurchaseOrderVo> queryPageList(PurchaseOrderBo bo, PageQuery pageQuery);

    PurchaseOrderDetailVo queryById(Long id);

    PurchaseOrderDetailVo markFailed(Long id, String reason);

    PurchaseOrderDetailVo cancel(Long id, String reason);

    PurchaseOrderDetailVo refund(Long id, String reason);

    PurchaseOrderDetailVo chargeback(Long id, String reason);
}
