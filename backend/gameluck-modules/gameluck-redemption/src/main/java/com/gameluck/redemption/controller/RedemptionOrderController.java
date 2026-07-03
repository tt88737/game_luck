package com.gameluck.redemption.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.redemption.domain.bo.RedemptionOrderBo;
import com.gameluck.redemption.domain.vo.RedemptionOrderVo;
import com.gameluck.redemption.service.IRedemptionOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Redemption order admin controller.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/redemption/order")
public class RedemptionOrderController extends BaseController {

    private final IRedemptionOrderService redemptionOrderService;

    @SaCheckPermission("redemption:order:list")
    @GetMapping("/list")
    public TableDataInfo<RedemptionOrderVo> list(RedemptionOrderBo bo, PageQuery pageQuery) {
        return redemptionOrderService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("redemption:order:query")
    @GetMapping("/{id}")
    public R<RedemptionOrderVo> getInfo(@PathVariable Long id) {
        return R.ok(redemptionOrderService.queryById(id));
    }

    @SaCheckPermission("redemption:order:add")
    @Log(title = "Redemption order add", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody RedemptionOrderBo bo) {
        return toAjax(redemptionOrderService.insertByBo(bo));
    }

    @SaCheckPermission("redemption:order:approve")
    @Log(title = "Redemption order approve", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/approve")
    public R<RedemptionOrderVo> approve(@PathVariable Long id, @RequestBody(required = false) RedemptionOrderBo bo) {
        return R.ok(redemptionOrderService.approve(id, bo == null ? null : bo.getAuditReason()));
    }

    @SaCheckPermission("redemption:order:reject")
    @Log(title = "Redemption order reject", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/reject")
    public R<RedemptionOrderVo> reject(@PathVariable Long id, @RequestBody(required = false) RedemptionOrderBo bo) {
        return R.ok(redemptionOrderService.reject(id, bo == null ? null : bo.getAuditReason()));
    }
}
