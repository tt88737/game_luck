package com.gameluck.redemption.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.redemption.domain.bo.RedemptionEligibilityPolicyBo;
import com.gameluck.redemption.domain.vo.RedemptionEligibilityPolicyVo;

public interface IRedemptionEligibilityPolicyService {

    TableDataInfo<RedemptionEligibilityPolicyVo> queryPageList(RedemptionEligibilityPolicyBo bo, PageQuery pageQuery);

    RedemptionEligibilityPolicyVo queryById(Long id);

    int insertByBo(RedemptionEligibilityPolicyBo bo);

    int updateByBo(RedemptionEligibilityPolicyBo bo);

    boolean isEligible(String tenantId, String currencyCode, String countryCode, String stateCode, String channel);
}
