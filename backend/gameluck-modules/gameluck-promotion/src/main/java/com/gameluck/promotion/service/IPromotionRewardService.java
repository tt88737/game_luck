package com.gameluck.promotion.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.promotion.domain.bo.PromotionClaimBo;
import com.gameluck.promotion.domain.bo.PromotionRewardBo;
import com.gameluck.promotion.domain.vo.PromotionClaimVo;
import com.gameluck.promotion.domain.vo.PromotionRewardVo;

import java.util.Collection;
import java.util.List;

/**
 * Promotion reward service.
 */
public interface IPromotionRewardService {

    TableDataInfo<PromotionRewardVo> queryPageList(PromotionRewardBo bo, PageQuery pageQuery);

    PromotionRewardVo queryById(Long id);

    List<PromotionRewardVo> queryList(PromotionRewardBo bo);

    Boolean insertByBo(PromotionRewardBo bo);

    Boolean updateByBo(PromotionRewardBo bo);

    Boolean deleteWithValidByIds(Collection<Long> ids);

    PromotionRewardVo updateStatus(Long id, String status);

    PromotionClaimVo claim(PromotionClaimBo bo);

    PromotionClaimVo claimDailyLoginReward(Long memberId);
}
