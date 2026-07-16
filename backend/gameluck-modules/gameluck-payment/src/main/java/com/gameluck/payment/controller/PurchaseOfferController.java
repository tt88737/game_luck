package com.gameluck.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.payment.domain.bo.PurchaseOfferBo;
import com.gameluck.payment.domain.vo.PurchaseOfferVo;
import com.gameluck.payment.service.IPurchaseOfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Purchase offer admin controller.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/payment/purchase-offer")
public class PurchaseOfferController extends BaseController {

    private final IPurchaseOfferService purchaseOfferService;

    @SaCheckPermission("payment:purchaseOffer:list")
    @GetMapping("/list")
    public TableDataInfo<PurchaseOfferVo> list(PurchaseOfferBo bo, PageQuery pageQuery) {
        return purchaseOfferService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("payment:purchaseOffer:query")
    @GetMapping("/{id}")
    public R<PurchaseOfferVo> getInfo(@PathVariable Long id) {
        return R.ok(purchaseOfferService.queryById(id));
    }

    @SaCheckPermission("payment:purchaseOffer:add")
    @Log(title = "购买产品新增", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody PurchaseOfferBo bo) {
        return toAjax(purchaseOfferService.insertByBo(bo) > 0);
    }

    @SaCheckPermission("payment:purchaseOffer:edit")
    @Log(title = "购买产品编辑", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody PurchaseOfferBo bo) {
        return toAjax(purchaseOfferService.updateByBo(bo));
    }
}
