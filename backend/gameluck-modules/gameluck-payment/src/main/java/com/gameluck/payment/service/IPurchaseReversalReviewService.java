package com.gameluck.payment.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.payment.domain.bo.PurchaseReversalReviewBo;
import com.gameluck.payment.domain.bo.PurchaseReversalReviewActionBo;
import com.gameluck.payment.domain.vo.PurchaseReversalReviewActionResultVo;
import com.gameluck.payment.domain.vo.PurchaseReversalReviewDetailVo;
import com.gameluck.payment.domain.vo.PurchaseReversalReviewVo;

public interface IPurchaseReversalReviewService {
    TableDataInfo<PurchaseReversalReviewVo> queryPageList(PurchaseReversalReviewBo bo, PageQuery pageQuery);
    PurchaseReversalReviewDetailVo queryByReversalNo(String reversalNo);
    PurchaseReversalReviewActionResultVo retry(String reversalNo, PurchaseReversalReviewActionBo bo);
    PurchaseReversalReviewActionResultVo acceptLoss(String reversalNo, PurchaseReversalReviewActionBo bo);
}
