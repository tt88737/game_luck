package com.gameluck.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.payment.domain.bo.PurchaseOrderBo;
import com.gameluck.payment.domain.vo.PurchaseOrderDetailVo;
import com.gameluck.payment.domain.vo.PurchaseOrderVo;
import com.gameluck.payment.service.IPurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Purchase order admin controller.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/payment/purchase-order")
public class PurchaseOrderController extends BaseController {

    private final IPurchaseOrderService purchaseOrderService;

    @SaCheckPermission("payment:purchaseOrder:list")
    @GetMapping("/list")
    public TableDataInfo<PurchaseOrderVo> list(PurchaseOrderBo bo, PageQuery pageQuery) {
        return purchaseOrderService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("payment:purchaseOrder:query")
    @GetMapping("/{id}")
    public R<PurchaseOrderDetailVo> getInfo(@PathVariable Long id) {
        return R.ok(purchaseOrderService.queryById(id));
    }

    @SaCheckPermission("payment:purchaseOrder:manual")
    @Log(title = "Purchase order mark failed", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/mark-failed")
    public R<PurchaseOrderDetailVo> markFailed(@PathVariable Long id, @RequestBody PurchaseOrderBo bo) {
        return R.ok(purchaseOrderService.markFailed(id, bo == null ? null : bo.getReason()));
    }

    @SaCheckPermission("payment:purchaseOrder:manual")
    @Log(title = "Purchase order cancel", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/cancel")
    public R<PurchaseOrderDetailVo> cancel(@PathVariable Long id, @RequestBody PurchaseOrderBo bo) {
        return R.ok(purchaseOrderService.cancel(id, bo == null ? null : bo.getReason()));
    }

    @SaCheckPermission("payment:purchaseOrder:manual")
    @Log(title = "Purchase order refund", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/refund")
    public R<PurchaseOrderDetailVo> refund(@PathVariable Long id, @RequestBody PurchaseOrderBo bo) {
        return R.ok(purchaseOrderService.refund(id, bo == null ? null : bo.getReason()));
    }

    @SaCheckPermission("payment:purchaseOrder:manual")
    @Log(title = "Purchase order chargeback", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/chargeback")
    public R<PurchaseOrderDetailVo> chargeback(@PathVariable Long id, @RequestBody PurchaseOrderBo bo) {
        return R.ok(purchaseOrderService.chargeback(id, bo == null ? null : bo.getReason()));
    }
}
