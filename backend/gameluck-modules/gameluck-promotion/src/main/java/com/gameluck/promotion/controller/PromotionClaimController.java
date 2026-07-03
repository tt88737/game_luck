package com.gameluck.promotion.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.promotion.domain.bo.PromotionClaimBo;
import com.gameluck.promotion.domain.vo.PromotionClaimVo;
import com.gameluck.promotion.service.IPromotionClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Promotion claim admin controller.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/promotion/claim")
public class PromotionClaimController extends BaseController {

    private final IPromotionClaimService promotionClaimService;

    @SaCheckPermission("promotion:reward:query")
    @GetMapping("/list")
    public TableDataInfo<PromotionClaimVo> list(PromotionClaimBo bo, PageQuery pageQuery) {
        return promotionClaimService.queryPageList(bo, pageQuery);
    }
}
