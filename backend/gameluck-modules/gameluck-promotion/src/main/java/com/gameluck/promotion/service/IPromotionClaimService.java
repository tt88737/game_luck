package com.gameluck.promotion.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.promotion.domain.bo.PromotionClaimBo;
import com.gameluck.promotion.domain.vo.PromotionClaimVo;

import java.util.List;

/**
 * Promotion claim service.
 */
public interface IPromotionClaimService {

    TableDataInfo<PromotionClaimVo> queryPageList(PromotionClaimBo bo, PageQuery pageQuery);

    List<PromotionClaimVo> queryList(PromotionClaimBo bo);
}
