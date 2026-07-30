package com.gameluck.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.payment.domain.bo.PurchaseReversalReviewActionBo;
import com.gameluck.payment.domain.bo.PurchaseReversalReviewBo;
import com.gameluck.payment.domain.vo.PurchaseReversalReviewActionResultVo;
import com.gameluck.payment.domain.vo.PurchaseReversalReviewDetailVo;
import com.gameluck.payment.domain.vo.PurchaseReversalReviewVo;
import com.gameluck.payment.service.IPurchaseReversalReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/payment/purchase-reversal-review")
public class PurchaseReversalReviewController {

    private final IPurchaseReversalReviewService reviewService;

    @SaCheckPermission("payment:reversalReview:list")
    @GetMapping("/list")
    public TableDataInfo<PurchaseReversalReviewVo> list(PurchaseReversalReviewBo bo, PageQuery pageQuery) {
        return reviewService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("payment:reversalReview:query")
    @GetMapping("/{reversalNo}")
    public R<PurchaseReversalReviewDetailVo> getInfo(@PathVariable String reversalNo) {
        return R.ok(reviewService.queryByReversalNo(reversalNo));
    }

    @SaCheckPermission("payment:reversalReview:retry")
    @Log(title = "Purchase reversal recovery retry", businessType = BusinessType.UPDATE)
    @PostMapping("/{reversalNo}/retry")
    public R<PurchaseReversalReviewActionResultVo> retry(@PathVariable String reversalNo,
                                                         @Validated @RequestBody PurchaseReversalReviewActionBo bo) {
        return R.ok(reviewService.retry(reversalNo, bo));
    }

    @SaCheckPermission("payment:reversalReview:acceptLoss")
    @Log(title = "Purchase reversal loss acceptance", businessType = BusinessType.UPDATE)
    @PostMapping("/{reversalNo}/accept-loss")
    public R<PurchaseReversalReviewActionResultVo> acceptLoss(@PathVariable String reversalNo,
                                                              @Validated @RequestBody PurchaseReversalReviewActionBo bo) {
        return R.ok(reviewService.acceptLoss(reversalNo, bo));
    }
}
