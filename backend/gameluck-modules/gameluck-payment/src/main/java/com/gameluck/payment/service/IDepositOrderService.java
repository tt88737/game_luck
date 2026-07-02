package com.gameluck.payment.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.payment.domain.bo.DepositOrderBo;
import com.gameluck.payment.domain.vo.DepositOrderVo;

import java.util.List;

/**
 * Deposit order service.
 */
public interface IDepositOrderService {

    TableDataInfo<DepositOrderVo> queryPageList(DepositOrderBo bo, PageQuery pageQuery);

    DepositOrderVo queryById(Long id);

    List<DepositOrderVo> queryList(DepositOrderBo bo);

    Boolean insertByBo(DepositOrderBo bo);

    DepositOrderVo simulateSuccess(Long id);

    Boolean cancel(Long id);
}
