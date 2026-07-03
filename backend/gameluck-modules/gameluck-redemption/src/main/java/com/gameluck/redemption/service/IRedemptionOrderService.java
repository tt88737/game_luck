package com.gameluck.redemption.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.redemption.domain.bo.RedemptionOrderBo;
import com.gameluck.redemption.domain.vo.RedemptionOrderVo;

import java.util.List;

/**
 * Redemption order service.
 */
public interface IRedemptionOrderService {

    TableDataInfo<RedemptionOrderVo> queryPageList(RedemptionOrderBo bo, PageQuery pageQuery);

    RedemptionOrderVo queryById(Long id);

    List<RedemptionOrderVo> queryList(RedemptionOrderBo bo);

    Boolean insertByBo(RedemptionOrderBo bo);

    RedemptionOrderVo approve(Long id, String reason);

    RedemptionOrderVo reject(Long id, String reason);
}
