package com.gameluck.promotion.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.promotion.domain.bo.PromotionClaimBo;
import com.gameluck.promotion.domain.bo.PromotionRewardBo;
import com.gameluck.promotion.domain.vo.PromotionClaimVo;
import com.gameluck.promotion.domain.vo.PromotionRewardVo;
import com.gameluck.promotion.service.IPromotionRewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * Promotion reward admin controller.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/promotion/reward")
public class PromotionRewardController extends BaseController {

    private final IPromotionRewardService promotionRewardService;

    @SaCheckPermission("promotion:reward:list")
    @GetMapping("/list")
    public TableDataInfo<PromotionRewardVo> list(PromotionRewardBo bo, PageQuery pageQuery) {
        return promotionRewardService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("promotion:reward:query")
    @GetMapping("/{id}")
    public R<PromotionRewardVo> getInfo(@PathVariable Long id) {
        return R.ok(promotionRewardService.queryById(id));
    }

    @SaCheckPermission("promotion:reward:add")
    @Log(title = "Promotion reward add", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody PromotionRewardBo bo) {
        return toAjax(promotionRewardService.insertByBo(bo));
    }

    @SaCheckPermission("promotion:reward:edit")
    @Log(title = "Promotion reward edit", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody PromotionRewardBo bo) {
        return toAjax(promotionRewardService.updateByBo(bo));
    }

    @SaCheckPermission("promotion:reward:remove")
    @Log(title = "Promotion reward remove", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(promotionRewardService.deleteWithValidByIds(Arrays.asList(ids)));
    }

    @SaCheckPermission("promotion:reward:edit")
    @Log(title = "Promotion reward status", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/status/{status}")
    public R<PromotionRewardVo> updateStatus(@PathVariable Long id, @PathVariable String status) {
        return R.ok(promotionRewardService.updateStatus(id, status));
    }

    @SaCheckPermission("promotion:reward:claim")
    @Log(title = "Promotion reward claim", businessType = BusinessType.INSERT)
    @PostMapping("/{id}/claim")
    public R<PromotionClaimVo> claim(@PathVariable Long id, @Validated @RequestBody PromotionClaimBo bo) {
        bo.setPromotionId(id);
        return R.ok(promotionRewardService.claim(bo));
    }
}
